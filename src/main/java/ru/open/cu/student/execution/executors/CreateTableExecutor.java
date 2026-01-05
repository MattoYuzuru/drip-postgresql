package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;

import java.io.IOException;
import java.util.List;

/**
 * Исполнитель CREATE TABLE.
 * Просто вызывает CatalogManager.createTable(name, columns).
 */
public class CreateTableExecutor implements Executor {

    private final CatalogManager catalogManager;
    private final TableDefinition tableDefinition;
    private final List<ColumnDefinition> columns;

    public CreateTableExecutor(CatalogManager catalogManager,
                               TableDefinition tableDefinition,
                               List<ColumnDefinition> columns) {
        this.catalogManager = catalogManager;
        this.tableDefinition = tableDefinition;
        this.columns = columns;
    }

    @Override
    public void open() {
    }

    @Override
    public Object next() throws IOException {
        catalogManager.createTable(
                tableDefinition.name,
                columns
        );
        return null;
    }

    @Override
    public void close() {
    }
}
