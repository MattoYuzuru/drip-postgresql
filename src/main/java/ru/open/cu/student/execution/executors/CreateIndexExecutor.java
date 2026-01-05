package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.operation.OperationManager;
import ru.open.cu.student.index.Index;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.index.btree.BPlusTreeIndexImpl;
import ru.open.cu.student.index.hash.HashIndexImpl;

/**
 * Создание индекса и его регистрация в реестре.
 */
public class CreateIndexExecutor implements Executor {

    private final OperationManager operationManager;
    private final IndexRegistry indexRegistry;
    private final String indexName;
    private final TableDefinition tableDefinition;
    private final ColumnDefinition columnDefinition;
    private final IndexType indexType;

    public CreateIndexExecutor(OperationManager operationManager,
                               IndexRegistry indexRegistry,
                               String indexName,
                               TableDefinition tableDefinition,
                               ColumnDefinition columnDefinition,
                               IndexType indexType) {
        this.operationManager = operationManager;
        this.indexRegistry = indexRegistry;
        this.indexName = indexName;
        this.tableDefinition = tableDefinition;
        this.columnDefinition = columnDefinition;
        this.indexType = indexType;
    }

    @Override
    public void open() {
    }

    @Override
    public Object next() {
        Index index = switch (indexType) {
            case HASH -> new HashIndexImpl(indexName, tableDefinition.name, columnDefinition.name);
            case BTREE -> new BPlusTreeIndexImpl(indexName, tableDefinition.name, columnDefinition.name);
        };
        indexRegistry.register(index);
        operationManager.buildIndex(index);
        return null;
    }

    @Override
    public void close() {
    }
}
