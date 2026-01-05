package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.operation.OperationManager;

import java.util.Iterator;
import java.util.List;

public class SeqScanExecutor implements Executor {

    private final OperationManager operationManager;
    private final TableDefinition table;
    private final List<String> outputSchema;

    private Iterator<?> it;

    public SeqScanExecutor(OperationManager operationManager,
                           TableDefinition table,
                           List<String> outputSchema) {
        this.operationManager = operationManager;
        this.table = table;
        this.outputSchema = outputSchema;
    }

    @Override
    public void open() {
        // Берём все строки, а наверх отдаём по одной
        List<Object> rows = operationManager.select(table.name, outputSchema);
        this.it = rows.iterator();
    }

    @Override
    public Object next() {
        if (it != null && it.hasNext()) return it.next();
        return null;
    }

    @Override
    public void close() {
        this.it = null;
    }
}
