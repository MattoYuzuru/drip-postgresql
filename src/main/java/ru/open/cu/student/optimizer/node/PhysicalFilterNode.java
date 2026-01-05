package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.ast.Expr;

/**
 * Физический узел Filter — фильтрация строк по предикату WHERE.
 * Работает поверх дочернего физического узла (child).
 */
public class PhysicalFilterNode extends PhysicalPlanNode {

    private final Expr predicate;
    private final PhysicalPlanNode child;

    public PhysicalFilterNode(Expr predicate, PhysicalPlanNode child) {
        super("PhysicalFilter");
        this.predicate = predicate;
        this.child = child;
        setOutputColumns(child.getOutputColumns());
    }

    public Expr getPredicate() { return predicate; }
    public PhysicalPlanNode getChild() { return child; }

    @Override
    public String prettyPrint(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("PhysicalFilter(").append(predicate).append(")\n");
        sb.append(child.prettyPrint(indent + "  "));
        return sb.toString();
    }
}