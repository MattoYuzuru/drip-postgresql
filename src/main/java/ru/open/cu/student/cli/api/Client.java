package ru.open.cu.student.cli.api;

import java.io.IOException;

/**
 * Клиентское приложение с простым REPL.
 */
public interface Client {
    void start() throws IOException;
}
