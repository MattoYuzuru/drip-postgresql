package ru.open.cu.student.cli.api;

/**
 * Движок выполнения SQL-команд.
 */
public interface Engine {
    String executeSql(String line);
}
