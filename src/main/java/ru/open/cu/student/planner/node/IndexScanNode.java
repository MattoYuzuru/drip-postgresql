package ru.open.cu.student.planner.node;

import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.IndexType;

import java.util.List;

public class IndexScanNode extends LogicalPlanNode {

    private final TableDefinition tableDefinition;
    private final String indexName;
    private final IndexType indexType;
    private final IndexPredicate predicate;

    public IndexScanNode(TableDefinition tableDefinition,
                         List<String> outputColumns,
                         String indexName,
                         IndexType indexType,
                         IndexPredicate predicate) {
        super("IndexScan");
        this.tableDefinition = tableDefinition;
        this.indexName = indexName;
        this.indexType = indexType;
        this.predicate = predicate;
        this.outputColumns = List.copyOf(outputColumns);
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
        return indent + "IndexScan(" + indexName + ", " + indexType + ")\n";
    }
}
