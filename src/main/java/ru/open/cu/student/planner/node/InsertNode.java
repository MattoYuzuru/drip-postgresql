package ru.open.cu.student.planner.node;

import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.ast.Expr;

import java.util.List;

/**
 * Логический узел INSERT INTO table VALUES (...).
 * Хранит готовый объект Table и список значений.
 */
public class InsertNode extends LogicalPlanNode {

    private final TableDefinition tableDefinition;
    private final List<Expr> values;  // Expr вместо Object

    public InsertNode(TableDefinition tableDefinition, List<Expr> values) {
        super("Insert");
        this.tableDefinition = tableDefinition;
        this.values = values;
        this.outputColumns = List.of(); // INSERT не возвращает строки
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    public List<Expr> getValues() {
        return values;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "Insert(" + tableDefinition.name + ", values=" + values + ")\n";
    }
}