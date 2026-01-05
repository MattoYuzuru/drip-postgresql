package ru.open.cu.student.optimizer;


import ru.open.cu.student.optimizer.node.*;
import ru.open.cu.student.planner.node.*;

public class OptimizerImpl implements Optimizer {

    @Override
    public PhysicalPlanNode optimize(LogicalPlanNode logicalPlan) {
        if (logicalPlan instanceof ru.open.cu.student.planner.node.CreateTableNode ln) {
            return new ru.open.cu.student.optimizer.node.PhysicalCreateNode(
                    ln.getTableDefinition(),
                    ln.getColumns()
            );
        } else if (logicalPlan instanceof ru.open.cu.student.planner.node.CreateIndexNode ln) {
            return new ru.open.cu.student.optimizer.node.PhysicalCreateIndexNode(
                    ln.getIndexName(),
                    ln.getTableDefinition(),
                    ln.getColumnDefinition(),
                    ln.getIndexType()
            );
        } else if (logicalPlan instanceof ru.open.cu.student.planner.node.ResetNode ln) {
            return new PhysicalResetNode(ln.getScope());
        } else if (logicalPlan instanceof InsertNode ln) {
            return new PhysicalInsertNode(ln.getTableDefinition(), ln.getValues());

        } else if (logicalPlan instanceof ru.open.cu.student.planner.node.IndexScanNode ln) {
            return new PhysicalIndexScanNode(
                    ln.getTableDefinition(),
                    ln.getIndexName(),
                    ln.getIndexType(),
                    ln.getPredicate(),
                    ln.getOutputColumns()
            );

        } else if (logicalPlan instanceof ru.open.cu.student.planner.node.ScanNode ln) {
            return new PhysicalSeqScanNode(ln.getTableDefinition(), ln.getOutputColumns());

        } else if (logicalPlan instanceof FilterNode ln) {
            PhysicalPlanNode child = optimize(ln.getChild());
            return new PhysicalFilterNode(ln.getPredicate(), child);

        } else if (logicalPlan instanceof ProjectNode ln) {
            PhysicalPlanNode child = optimize(ln.getChild());
            return new PhysicalProjectNode(ln.getTargetList(), child);
        }

        throw new UnsupportedOperationException(
                "Unsupported logical node type: " + logicalPlan.getClass().getSimpleName()
        );
    }
}
