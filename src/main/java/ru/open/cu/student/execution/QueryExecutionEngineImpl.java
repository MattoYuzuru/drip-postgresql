package ru.open.cu.student.execution;


import ru.open.cu.student.execution.executors.Executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * QueryExecutionEngineImpl — точка входа для выполнения уже подготовленного запроса.
 * Он принимает готовый Executor (верхний в дереве исполнителей),
 * выполняет стандартный цикл open → next* → close и возвращает результаты.
 */
public class QueryExecutionEngineImpl implements QueryExecutionEngine {

    @Override
    public List<Object> execute(Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("Executor is null");
        }

        List<Object> results = new ArrayList<>();

        try {
            executor.open();

            Object row;
            while ((row = executor.next()) != null) {
                results.add(row);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            executor.close();
        }

        return results;
    }
}