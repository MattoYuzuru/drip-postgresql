package ru.open.cu.student.memory.manager;

import ru.open.cu.student.memory.page.HeapPage;
import ru.open.cu.student.memory.page.Page;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static ru.open.cu.student.memory.page.HeapPage.PAGE_SIZE;

public class HeapPageFileManager implements PageFileManager {

    @Override
    public void write(Page page, Path path) {
        byte[] bytes = page.bytes();
        if (bytes == null || bytes.length != PAGE_SIZE) {
            throw new IllegalArgumentException("Page bytes must be exactly " + PAGE_SIZE + " bytes");
        }

        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            int pageId = page.getPageId();

            try (FileChannel channel = FileChannel.open(
                    path,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE)) {

                if (pageId >= 0) {
                    long position = ((long) pageId) * PAGE_SIZE;
                    channel.write(ByteBuffer.wrap(bytes), position);
                } else {
                    channel.position(channel.size());
                    channel.write(ByteBuffer.wrap(bytes));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write page to file: " + path, e);
        }
    }

    @Override
    public Page read(int pageId, Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + path);
        }

        ByteBuffer buf = ByteBuffer.allocate(PAGE_SIZE);

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long position = ((long) pageId) * PAGE_SIZE;
            int read = channel.read(buf, position);
            if (read < PAGE_SIZE) {
                throw new IllegalArgumentException("Page " + pageId + " is out of file bounds or incomplete");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read page " + pageId + " from file: " + path, e);
        }

        Page page = new HeapPage(pageId, buf.array());
        if (!page.isValid()) {
            throw new IllegalStateException("Invalid page signature at id=" + pageId);
        }
        return page;
    }
}
