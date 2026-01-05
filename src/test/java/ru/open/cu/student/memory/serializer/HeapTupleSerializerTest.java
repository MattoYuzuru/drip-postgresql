package ru.open.cu.student.memory.serializer;

import org.junit.jupiter.api.Test;
import ru.open.cu.student.memory.model.DataType;
import ru.open.cu.student.memory.model.HeapTuple;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeapTupleSerializerTest {

    private final HeapTupleSerializer serializer = new HeapTupleSerializer();

    @Test
    void long_roundTrip_extremes() {
        long[] values = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 42L, -42L};
        for (long v : values) {
            HeapTuple t = serializer.serialize(v, DataType.INT64);
            assertEquals(DataType.INT64, t.type());
            assertEquals(8, t.data().length);

            Long restored = serializer.deserialize(t);
            assertEquals(v, restored);
        }
    }

    @Test
    void varchar_roundTrip_basic_and_utf8() {
        String[] values = {"", "a", "hello", "Привет", "世界", "emoji🙂"};
        for (String s : values) {
            HeapTuple t = serializer.serialize(s, DataType.VARCHAR);
            assertEquals(DataType.VARCHAR, t.type());

            String restored = serializer.deserialize(t);
            assertEquals(s, restored);
        }
    }

    @Test
    void varchar_255_bytes_boundary() {
        String s255 = "a".repeat(255);
        HeapTuple t = serializer.serialize(s255, DataType.VARCHAR);
        assertEquals(1 + 255, t.data().length);
        assertEquals(255, t.data()[0] & 0xFF);

        String restored = serializer.deserialize(t);
        assertEquals(s255, restored);
    }
}

