package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.operation.OperationManager;

/**
 * Сброс базы данных: удаление файлов и очистка состояния.
 */
public class ResetExecutor implements Executor {

    private final OperationManager operationManager;
    private final String scope;

    public ResetExecutor(OperationManager operationManager, String scope) {
        this.operationManager = operationManager;
        this.scope = scope;
    }

    @Override
    public void open() {
    }

    @Override
    public Object next() {
        operationManager.reset(scope);
        return null;
    }

    @Override
    public void close() {
    }
}
