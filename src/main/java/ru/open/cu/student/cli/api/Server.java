package ru.open.cu.student.cli.api;

import java.io.IOException;

/**
 * Серверный процесс, принимающий клиентские соединения.
 */
public interface Server {
    void start() throws IOException;
}
