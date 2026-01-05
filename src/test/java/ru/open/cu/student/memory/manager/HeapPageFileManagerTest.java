package ru.open.cu.student.memory.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.memory.page.HeapPage;
import ru.open.cu.student.memory.page.Page;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class HeapPageFileManagerTest {

    @Test
    void write_then_read_roundTrip(@TempDir Path tempDir) {
        Path path = tempDir.resolve("db.dat");
        PageFileManager fm = new HeapPageFileManager();

        Page p0 = new HeapPage(0);
        p0.write(new byte[]{1, 2, 3});
        p0.write(new byte[]{4});

        fm.write(p0, path);

        Page r0 = fm.read(0, path);
        assertTrue(r0.isValid());
        assertEquals(2, r0.size());
        assertArrayEquals(new byte[]{1, 2, 3}, r0.read(0));
        assertArrayEquals(new byte[]{4}, r0.read(1));
    }

    @Test
    void read_nonExisting_file_throws(@TempDir Path tempDir) {
        Path path = tempDir.resolve("missing.dat");
        PageFileManager fm = new HeapPageFileManager();
        assertThrows(IllegalArgumentException.class, () -> fm.read(0, path));
    }

    @Test
    void read_page_out_of_bounds_throws(@TempDir Path tempDir) {
        Path path = tempDir.resolve("db.dat");
        PageFileManager fm = new HeapPageFileManager();

        Page p0 = new HeapPage(0);
        p0.write(new byte[]{10});
        fm.write(p0, path);

        assertThrows(IllegalArgumentException.class, () -> fm.read(1, path));
    }

    @Test
    void invalid_signature_on_disk_throws(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("db.dat");
        Files.write(path, new byte[HeapPage.PAGE_SIZE]);

        PageFileManager fm = new HeapPageFileManager();
        assertThrows(IllegalStateException.class, () -> fm.read(0, path));
    }

    @Test
    void append_negative_pageId_writes_to_end(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("db.dat");
        PageFileManager fm = new HeapPageFileManager();

        Page p0 = new HeapPage(0);
        p0.write(new byte[]{1});
        fm.write(p0, path);

        Page append = new HeapPage(-1);
        append.write(new byte[]{2, 3});
        fm.write(append, path);

        long size = Files.size(path);
        int lastId = (int) (size / HeapPage.PAGE_SIZE) - 1;
        assertEquals(1, lastId);

        Page rLast = fm.read(lastId, path);
        assertEquals(1, rLast.size());
        assertArrayEquals(new byte[]{2, 3}, rLast.read(0));
    }
}

