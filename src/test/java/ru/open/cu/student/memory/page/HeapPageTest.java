package ru.open.cu.student.memory.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HeapPageTest {

    @Test
    void new_page_is_valid_and_fixed_size() {
        Page page = new HeapPage(0);
        assertTrue(page.isValid());
        assertEquals(0, page.size());
        assertEquals(HeapPage.PAGE_SIZE, page.bytes().length);
    }

    @Test
    void write_and_read_multiple_records_in_order() {
        Page page = new HeapPage(5);
        byte[] a = new byte[]{1, 2, 3};
        byte[] b = new byte[]{4};
        byte[] c = new byte[]{10, 11, 12, 13, 14};

        page.write(a);
        page.write(b);
        page.write(c);

        assertEquals(3, page.size());
        assertArrayEquals(a, page.read(0));
        assertArrayEquals(b, page.read(1));
        assertArrayEquals(c, page.read(2));
    }

    @Test
    void fill_until_full_then_throw() {
        Page page = new HeapPage(7);
        int count = 0;
        byte[] one = new byte[]{42};
        try {
            while (true) {
                page.write(one);
                count++;
            }
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().toLowerCase().contains("not enough space"));
        }
        assertTrue(page.size() > 0);
        assertEquals(count, page.size());
    }

    @Test
    void supports_various_record_sizes() {
        Page page = new HeapPage(9);
        byte[] x = new byte[1];
        byte[] y = new byte[128];
        byte[] z = new byte[1024];
        for (int i = 0; i < y.length; i++) y[i] = (byte) i;
        for (int i = 0; i < z.length; i++) z[i] = (byte) (255 - (i % 256));

        page.write(x);
        page.write(y);
        page.write(z);

        assertEquals(3, page.size());
        assertArrayEquals(x, page.read(0));
        assertArrayEquals(y, page.read(1));
        assertArrayEquals(z, page.read(2));
    }
}

