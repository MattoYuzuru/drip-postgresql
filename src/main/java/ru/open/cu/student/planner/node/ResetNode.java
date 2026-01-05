package ru.open.cu.student.planner.node;

public class ResetNode extends LogicalPlanNode {

    private final String scope;

    public ResetNode(String scope) {
        super("Reset");
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "Reset(" + (scope == null ? "ALL" : scope) + ")\n";
    }
}
