package ru.open.cu.student.index;

import java.util.Objects;

/**
 * Tuple identifier.
 * <p>
 * The current storage engine keeps rows in append-only files, so we model
 * {@code pageId} as a logical row index (slotId is reserved for future use).
 */
public final class TID implements Comparable<TID> {

    private final int pageId;
    private final short slotId;

    public TID(int pageId, short slotId) {
        this.pageId = pageId;
        this.slotId = slotId;
    }

    public static TID ofRowIndex(int rowIndex) {
        return new TID(rowIndex, (short) 0);
    }

    public int getPageId() {
        return pageId;
    }

    public long linearAddress() {
        return ((long) pageId << 32) | (slotId & 0xFFFF_FFFFL);
    }

    @Override
    public int compareTo(TID o) {
        return Long.compare(this.linearAddress(), o.linearAddress());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TID tid)) return false;
        return pageId == tid.pageId && slotId == tid.slotId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, slotId);
    }

    @Override
    public String toString() {
        return "TID{" +
                "pageId=" + pageId +
                ", slotId=" + slotId +
                '}';
    }
}
