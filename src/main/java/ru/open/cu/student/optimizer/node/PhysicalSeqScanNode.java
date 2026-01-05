package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.TableDefinition;

import java.util.List;

public class PhysicalSeqScanNode extends PhysicalPlanNode {

    private final TableDefinition table;

    public PhysicalSeqScanNode(TableDefinition table, List<String> outputColumns) {
        super("PhysicalSeqScan");
        this.table = table;
        setOutputColumns(outputColumns);
    }

    public TableDefinition getTable() { return table; }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalSeqScan(" + table.name + ")\n";
    }
}
