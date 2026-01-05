package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;

import java.util.List;

/**
 * CREATE TABLE node — создание таблицы в хранилище.
 * Содержит метаданные таблицы и список колонок.
 */
public class PhysicalCreateNode extends PhysicalPlanNode {

    private final TableDefinition tableDefinition;
    private final List<ColumnDefinition> columns;

    public PhysicalCreateNode(TableDefinition tableDefinition, List<ColumnDefinition> columns) {
        super("PhysicalCreate");
        this.tableDefinition = tableDefinition;
        this.columns = List.copyOf(columns);
    }

    public TableDefinition getTableDefinition() { return tableDefinition; }
    public List<ColumnDefinition> getColumns() { return columns; }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalCreate(" + tableDefinition.name + ")\n";
    }
}
