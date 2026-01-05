package ru.open.cu.student.planner.node;

import ru.open.cu.student.ast.Expr;

/**
 * Логический узел Filter - фильтрация строк по предикату.
 */
public class FilterNode extends LogicalPlanNode {

    private final Expr predicate; // WHERE-условие (резолвнутое выражение)
    private final LogicalPlanNode child;

    public FilterNode(Expr predicate, LogicalPlanNode child) {
        super("Filter");
        this.predicate = predicate;
        this.child = child;
        // Схема на выходе совпадает со входной
        this.outputColumns = child.getOutputColumns();
    }

    public Expr getPredicate() {
        return predicate;
    }

    public LogicalPlanNode getChild() {
        return child;
    }

    @Override
    public String prettyPrint(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Filter(").append(predicate).append(")\n");
        sb.append(child.prettyPrint(indent + "  "));
        return sb.toString();
    }
}