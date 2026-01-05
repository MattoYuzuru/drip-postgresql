package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.ast.TargetEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Физический узел Project — формирует итоговый SELECT-список.
 * Вызывает выполнение нижнего узла и выбирает указанные колонки/выражения.
 */
public class PhysicalProjectNode extends PhysicalPlanNode {

    private final List<TargetEntry> targetList;
    private final PhysicalPlanNode child;

    public PhysicalProjectNode(List<TargetEntry> targetList, PhysicalPlanNode child) {
        super("PhysicalProject");
        this.targetList = List.copyOf(targetList);
        this.child = child;
        // Имена выходных колонок — по алиасам/выражениям
        List<String> out = new ArrayList<>();
        for (TargetEntry te : targetList) {
            out.add(te.alias != null && !te.alias.isBlank() ? te.alias : te.expr.toString());
        }
        setOutputColumns(out);
    }

    public List<TargetEntry> getTargetList() { return targetList; }
    public PhysicalPlanNode getChild() { return child; }

    @Override
    public String prettyPrint(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("PhysicalProject(").append(String.join(", ", getOutputColumns())).append(")\n");
        sb.append(child.prettyPrint(indent + "  "));
        return sb.toString();
    }
}