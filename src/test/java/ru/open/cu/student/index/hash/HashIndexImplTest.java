package ru.open.cu.student.index.hash;

import org.junit.jupiter.api.Test;
import ru.open.cu.student.index.TID;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashIndexImplTest {

    @Test
    void insertsAndSearches() {
        HashIndexImpl index = new HashIndexImpl("idx", "users", "id", 2);
        index.insert(1L, TID.ofRowIndex(0));
        index.insert(2L, TID.ofRowIndex(1));
        index.insert(17L, TID.ofRowIndex(2)); // triggers split because hash collides with 1

        List<TID> result = index.search(2L);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getPageId());
        assertEquals(3, index.getRecordCount());
    }
}
