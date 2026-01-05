package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.planner.node.IndexPredicate;

import java.util.List;

public class PhysicalIndexScanNode extends PhysicalPlanNode {

    private final TableDefinition tableDefinition;
    private final String indexName;
    private final IndexType indexType;
    private final IndexPredicate predicate;

    public PhysicalIndexScanNode(TableDefinition tableDefinition,
                                 String indexName,
                                 IndexType indexType,
                                 IndexPredicate predicate,
                                 List<String> outputColumns) {
        super("PhysicalIndexScan");
        this.tableDefinition = tableDefinition;
        this.indexName = indexName;
        this.indexType = indexType;
        this.predicate = predicate;
        setOutputColumns(outputColumns);
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    public String getIndexName() {
        return indexName;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public IndexPredicate getPredicate() {
        return predicate;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalIndexScan(" + indexName + ", " + indexType + ")\n";
    }
}
