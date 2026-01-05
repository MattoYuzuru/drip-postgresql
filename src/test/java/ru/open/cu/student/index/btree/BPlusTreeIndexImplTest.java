package ru.open.cu.student.index.btree;

import org.junit.jupiter.api.Test;
import ru.open.cu.student.index.TID;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BPlusTreeIndexImplTest {

    @Test
    void supportsEqualityAndRanges() {
        BPlusTreeIndexImpl index = new BPlusTreeIndexImpl("idx", "users", "age", 3);
        for (int i = 0; i < 50; i++) {
            index.insert(i, TID.ofRowIndex(i));
        }

        assertEquals(1, index.search(10).size());
        assertEquals(50, index.scanAll().size());
        assertEquals(41, index.searchGreaterThan(9, true).size());
        assertEquals(11, index.searchLessThan(10, true).size());

        List<TID> range = index.rangeSearch(10, true, 20, true);
        assertEquals(11, range.size());
    }
}
