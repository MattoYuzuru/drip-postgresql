package ru.open.cu.student.memory.serializer;

import ru.open.cu.student.memory.model.DataType;
import ru.open.cu.student.memory.model.HeapTuple;

import java.nio.charset.StandardCharsets;

public class HeapTupleSerializer implements TupleSerializer {
    @Override
    public <T> HeapTuple serialize(T value, DataType dataType) {
        return switch (dataType) {
            case INT64 -> new HeapTuple(longToBytes((Long) value), DataType.INT64);
            case VARCHAR -> {
                byte[] strBytes = value.toString().getBytes(StandardCharsets.UTF_8);

                byte[] result = new byte[1 + strBytes.length];
                result[0] = (byte) strBytes.length;
                System.arraycopy(strBytes, 0, result, 1, strBytes.length);
                yield new HeapTuple(result, DataType.VARCHAR);
            }
        };
    }

    @Override
    public <T> T deserialize(HeapTuple tuple) {
        return switch (tuple.type()) {
            case INT64 -> (T) Long.valueOf(bytesToLong(tuple.data()));
            case VARCHAR -> {
                byte[] data = tuple.data();
                int length = data[0] & 0xFF;
                String str = new String(data, 1, length, StandardCharsets.UTF_8);
                yield (T) str;
            }
        };
    }

    public static byte[] longToBytes(long value) {
        return new byte[]{
                (byte) (value >>> 56),
                (byte) (value >>> 48),
                (byte) (value >>> 40),
                (byte) (value >>> 32),
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    public static long bytesToLong(byte[] bytes) {
        return ((long) (bytes[0] & 0xFF) << 56) |
                ((long) (bytes[1] & 0xFF) << 48) |
                ((long) (bytes[2] & 0xFF) << 40) |
                ((long) (bytes[3] & 0xFF) << 32) |
                ((long) (bytes[4] & 0xFF) << 24) |
                ((long) (bytes[5] & 0xFF) << 16) |
                ((long) (bytes[6] & 0xFF) << 8) |
                ((long) (bytes[7] & 0xFF));
    }

}
