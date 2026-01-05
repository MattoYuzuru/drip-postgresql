package ru.open.cu.student.planner.node;


import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;

import java.util.List;

/**
 * Логический узел CREATE TABLE.
 * Хранит готовый объект Table.
 */
public class CreateTableNode extends LogicalPlanNode {

    private final TableDefinition tableDefinition;
    private final List<ColumnDefinition> columns;

    public CreateTableNode(TableDefinition tableDefinition, List<ColumnDefinition> columns) {
        super("CreateTable");
        this.tableDefinition = tableDefinition;
        this.outputColumns = List.of(); // CREATE не возвращает данных
        this.columns = List.copyOf(columns);
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    public List<ColumnDefinition> getColumns() {
        return columns;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "CreateTable(" + tableDefinition.name + ")\n";
    }
}