package ru.open.cu.student.execution;

import ru.open.cu.student.execution.executors.Executor;

import java.util.List;

public interface QueryExecutionEngine {
    List<Object> execute(Executor executor);
}
