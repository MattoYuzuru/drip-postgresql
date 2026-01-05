package ru.open.cu.student.memory.io;

import ru.open.cu.student.memory.buffer.BufferPoolManager;
import ru.open.cu.student.memory.model.BufferSlot;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * startBackgroundWriter(): каждые flushIntervalMs берёт грязные страницы и пишет их батчами. <br>
 * startCheckPointer(): периодически вызывает flushAllPages() каждые checkpointIntervalMs. <br>
 * Для правильной работы требуется передать BufferPoolManage. <br>
 */
public class DefaultDirtyPageWriter implements DirtyPageWriter {

    private final BufferPoolManager bufferPoolManager;
    private final ScheduledExecutorService svc;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final long flushIntervalMs;
    private final int batchSize;
    private final long checkpointIntervalMs;
    private ScheduledFuture<?> backgroundTask;
    private ScheduledFuture<?> checkpointTask;

    public DefaultDirtyPageWriter(BufferPoolManager bufferPoolManager,
                                  long flushIntervalMs,
                                  int batchSize,
                                  long checkpointIntervalMs) {
        this.bufferPoolManager = bufferPoolManager;
        this.flushIntervalMs = Math.max(100, flushIntervalMs);
        this.batchSize = Math.max(1, batchSize);
        this.checkpointIntervalMs = Math.max(1000, checkpointIntervalMs);
        this.svc = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DirtyPageWriter");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void startBackgroundWriter() {
        if (!running.compareAndSet(false, true)) return;
        backgroundTask = svc.scheduleWithFixedDelay(() -> {
            try {
                List<BufferSlot> dirty = bufferPoolManager.getDirtyPages();
                if (dirty == null || dirty.isEmpty()) return;
                // batch flush
                int idx = 0;
                while (idx < dirty.size()) {
                    int end = Math.min(idx + batchSize, dirty.size());
                    List<BufferSlot> batch = dirty.subList(idx, end);
                    // flush всех страниц через манагер
                    for (BufferSlot slot : batch) {
                        if (slot != null && slot.isDirty()) {
                            bufferPoolManager.flushPage(slot.getPageId());
                        }
                    }
                    idx = end;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 0, flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startCheckPointer() {
        if (!running.get()) {
            startBackgroundWriter();
        }
        checkpointTask = svc.scheduleWithFixedDelay(() -> {
            try {
                bufferPoolManager.flushAllPages();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, checkpointIntervalMs, checkpointIntervalMs, TimeUnit.MILLISECONDS);
    }

    // себе
    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (backgroundTask != null) backgroundTask.cancel(false);
        if (checkpointTask != null) checkpointTask.cancel(false);
        svc.shutdownNow();
    }
}
