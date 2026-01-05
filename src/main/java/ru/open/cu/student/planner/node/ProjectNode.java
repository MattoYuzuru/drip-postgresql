package ru.open.cu.student.planner.node;

import ru.open.cu.student.ast.ColumnRef;
import ru.open.cu.student.ast.Expr;
import ru.open.cu.student.ast.TargetEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Логический узел Project - выборка указанных колонок/выражений из результата нижнего узла.
 */
public class ProjectNode extends LogicalPlanNode {

    private final List<TargetEntry> targetList;
    private final LogicalPlanNode child;

    public ProjectNode(List<TargetEntry> targetList, LogicalPlanNode child) {
        super("Project");
        this.targetList = List.copyOf(targetList);
        this.child = child;
        this.outputColumns = computeOutputColumns(targetList);
    }

    public List<TargetEntry> getTargetList() {
        return targetList;
    }

    public LogicalPlanNode getChild() {
        return child;
    }

    private static List<String> computeOutputColumns(List<TargetEntry> targets) {
        List<String> out = new ArrayList<>();
        for (TargetEntry te : targets) {
            String name = te.alias;
            if (name == null || name.isBlank()) {
                Expr e = te.expr;
                if (e instanceof ColumnRef cr) {
                    name = (cr.table == null || cr.table.isBlank()) ? cr.column : cr.table + "." + cr.column;
                } else {
                    name = e.toString();
                }
            }
            out.add(name);
        }
        return out;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "Project(" + String.join(", ", outputColumns) + ")\n" +
                child.prettyPrint(indent + "  ");
    }
}
