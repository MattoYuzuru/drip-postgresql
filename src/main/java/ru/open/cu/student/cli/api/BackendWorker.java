package ru.open.cu.student.cli.api;

/**
 * Backend-поток — обслуживает одно клиентское соединение.
 */
public interface BackendWorker extends Runnable {
    @Override
    void run();
}
