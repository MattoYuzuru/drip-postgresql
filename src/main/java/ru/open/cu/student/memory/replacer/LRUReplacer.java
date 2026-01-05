package ru.open.cu.student.memory.replacer;

import ru.open.cu.student.memory.model.BufferSlot;

import java.util.*;

/**
 * Простая LRU - хранит непинутые слоты в порядке доступа. <br>
 * push(bufferSlot) - добавляет/обновляет <br>
 * delete(pageId) - удаляет из кандидатов. <br>
 * pickVictim() - возвращает
 */
public class LRUReplacer implements Replacer {

    // сохраняет порядок вставки.
    private final LinkedHashMap<Integer, BufferSlot> map = new LinkedHashMap<>();

    @Override
    public synchronized void push(BufferSlot bufferSlot) {
        if (bufferSlot == null) return;
        if (bufferSlot.isPinned()) {
            map.remove(bufferSlot.getPageId());
            return;
        }
        // убрать старые вхождения если есть, положить в конец
        map.remove(bufferSlot.getPageId());
        map.put(bufferSlot.getPageId(), bufferSlot);
    }

    @Override
    public synchronized void delete(int pageId) {
        map.remove(pageId);
    }

    @Override
    public synchronized BufferSlot pickVictim() {
        Iterator<Map.Entry<Integer, BufferSlot>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BufferSlot> e = it.next();
            BufferSlot slot = e.getValue();
            if (slot.isPinned()) {
                it.remove();
                continue;
            }
            it.remove();
            return slot;
        }
        return null;
    }

    @Override
    public synchronized void reset() {
        map.clear();
    }
}
