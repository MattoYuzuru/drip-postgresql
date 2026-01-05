package ru.open.cu.student.execution.executors;

import java.io.IOException;

/**
 * Базовый интерфейс исполнителя (Executor).
 *
 * Каждый Executor реализует итераторный интерфейс:
 *  - open() — инициализация ресурсов
 *  - next() — получение следующей строки результата
 *  - close() — освобождение ресурсов
 */
public interface Executor {
    void open();
    Object next() throws IOException;
    void close();
}