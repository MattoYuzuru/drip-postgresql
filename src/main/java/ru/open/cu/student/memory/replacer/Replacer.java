package ru.open.cu.student.memory.replacer;

import ru.open.cu.student.memory.model.BufferSlot;

public interface Replacer {
    void push(BufferSlot bufferSlot);
    void delete(int pageId);
    BufferSlot pickVictim();

    default void reset() {
    }
}
