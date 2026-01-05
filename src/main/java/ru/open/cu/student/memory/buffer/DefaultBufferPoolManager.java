package ru.open.cu.student.memory.buffer;

import ru.open.cu.student.memory.manager.PageFileManager;
import ru.open.cu.student.memory.model.BufferSlot;
import ru.open.cu.student.memory.page.HeapPage;
import ru.open.cu.student.memory.page.Page;
import ru.open.cu.student.memory.replacer.Replacer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultBufferPoolManager implements BufferPoolManager {

    private final int poolSize;
    private final PageFileManager pageFileManager;
    private final Replacer replacer;
    private final Map<Integer, BufferSlot> pageTable;
    private final List<BufferSlot> buffer;
    private final Path dbFilePath;
    private final ReentrantReadWriteLock lock;

    public DefaultBufferPoolManager(int poolSize, PageFileManager pageFileManager, Replacer replacer, Path dbFilePath) {
        if (poolSize <= 0) throw new IllegalArgumentException("Размер пула должен быть больше 0");
        this.poolSize = poolSize;
        this.pageFileManager = Objects.requireNonNull(pageFileManager);
        this.replacer = Objects.requireNonNull(replacer);
        this.pageTable = new HashMap<>();
        this.buffer = new ArrayList<>(poolSize);
        this.dbFilePath = Objects.requireNonNull(dbFilePath);
        this.lock = new ReentrantReadWriteLock();
    }

    public DefaultBufferPoolManager(int poolSize, PageFileManager pageFileManager, Replacer replacer) {
        this(poolSize, pageFileManager, replacer, Paths.get("data.db"));
    }

    @Override
    public BufferSlot getPage(int pageId) {
        lock.writeLock().lock();
        try {
            // если страница есть в буфере, просто вернем ее
            BufferSlot existing = pageTable.get(pageId);
            if (existing != null) {
                existing.incrementUsage();
                // пометить слот как недавно используемый, если не запинен
                if (!existing.isPinned()) {
                    replacer.push(existing);
                }
                return existing;
            }

            // если нет, нужно забрать его с диска
            BufferSlot slotToUse;

            if (buffer.size() < poolSize) {
                // делаем слот, куда позже добавим страницу из диска
                Page page = safeReadPage(pageId);
                slotToUse = new BufferSlot(pageId, page);
                buffer.add(slotToUse);
            } else {
                // если место занято, нужно освободить слот
                BufferSlot victim = replacer.pickVictim();
                if (victim == null) {
                    // если все запинено
                    throw new RuntimeException("Все страницы закреплены, невозможно найти жертву");
                }

                // еще проверка жертвы на пин
                if (victim.isPinned()) {
                    throw new IllegalStateException("Цель закреплена");
                }

                // если нужно перезаписать данные, флашим
                if (victim.isDirty()) {
                    pageFileManager.write(victim.getPage(), dbFilePath);
                    victim.setDirty(false);
                }

                // убираем жертву
                pageTable.remove(victim.getPageId());

                // переиспользуем слот
                Page newPage = safeReadPage(pageId);
                int idx = buffer.indexOf(victim);
                BufferSlot newSlot = new BufferSlot(pageId, newPage);
                if (idx >= 0) {
                    buffer.set(idx, newSlot);
                } else {
                    // fallback
                    buffer.remove(victim);
                    buffer.add(newSlot);
                }
                slotToUse = newSlot;
            }

            pageTable.put(pageId, slotToUse);
            slotToUse.incrementUsage();
            if (!slotToUse.isPinned()) {
                replacer.push(slotToUse);
            }

            return slotToUse;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Page safeReadPage(int pageId) {
        try {
            return pageFileManager.read(pageId, dbFilePath);
        } catch (IllegalArgumentException ex) {
            return new HeapPage(pageId);
        }
    }

    @Override
    public void updatePage(int pageId, Page page) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = pageTable.get(pageId);
            if (slot == null) {
                throw new IllegalArgumentException("Страница " + pageId + " не найдена в буфере");
            }
            slot.setPage(page);
            slot.setDirty(true);
            slot.incrementUsage();
            // обновленная станица недавно использована => обновить replacer
            if (!slot.isPinned()) {
                replacer.push(slot);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void pinPage(int pageId) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = pageTable.get(pageId);
            if (slot == null) throw new IllegalArgumentException("Страница " + pageId + " не найдена в буфере");
            slot.setPinned(true);
            replacer.delete(pageId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void flushPage(int pageId) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = pageTable.get(pageId);
            if (slot != null && slot.isDirty()) {
                pageFileManager.write(slot.getPage(), dbFilePath);
                slot.setDirty(false);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void flushAllPages() {
        lock.writeLock().lock();
        try {
            for (BufferSlot slot : buffer) {
                if (slot.isDirty()) {
                    pageFileManager.write(slot.getPage(), dbFilePath);
                    slot.setDirty(false);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<BufferSlot> getDirtyPages() {
        lock.readLock().lock();
        try {
            List<BufferSlot> dirty = new ArrayList<>();
            for (BufferSlot slot : buffer) {
                if (slot.isDirty()) {
                    dirty.add(slot);
                }
            }
            return dirty;
        } finally {
            lock.readLock().unlock();
        }
    }

    // тулзы
    public int getPoolSize() {
        return poolSize;
    }

    public Path getDbFilePath() {
        return dbFilePath;
    }

    public Collection<BufferSlot> getAllSlots() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(buffer);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void unpinPage(int pageId) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = pageTable.get(pageId);
            if (slot == null) throw new IllegalArgumentException("Страница " + pageId + " не найдена в буфере");
            slot.setPinned(false);
            replacer.push(slot);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void reset() {
        lock.writeLock().lock();
        try {
            pageTable.clear();
            buffer.clear();
            replacer.reset();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
