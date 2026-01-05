package ru.open.cu.student.optimizer.node;

public class PhysicalResetNode extends PhysicalPlanNode {

    private final String scope;

    public PhysicalResetNode(String scope) {
        super("PhysicalReset");
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalReset(" + (scope == null ? "ALL" : scope) + ")\n";
    }
}
