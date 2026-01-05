package ru.open.cu.student.memory.buffer;

import ru.open.cu.student.memory.model.BufferSlot;
import ru.open.cu.student.memory.page.Page;

import java.util.List;

public interface BufferPoolManager {
    BufferSlot getPage(int pageId);

    void updatePage(int pageId, Page page);

    void pinPage(int pageId);

    void flushPage(int pageId);

    void flushAllPages();

    List<BufferSlot> getDirtyPages();

    default void reset() {
    }
}
