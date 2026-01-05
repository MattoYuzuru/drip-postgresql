package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.IndexType;

public class PhysicalCreateIndexNode extends PhysicalPlanNode {

    private final String indexName;
    private final TableDefinition tableDefinition;
    private final ColumnDefinition columnDefinition;
    private final IndexType indexType;

    public PhysicalCreateIndexNode(String indexName,
                                   TableDefinition tableDefinition,
                                   ColumnDefinition columnDefinition,
                                   IndexType indexType) {
        super("PhysicalCreateIndex");
        this.indexName = indexName;
        this.tableDefinition = tableDefinition;
        this.columnDefinition = columnDefinition;
        this.indexType = indexType;
    }

    public String getIndexName() {
        return indexName;
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    public ColumnDefinition getColumnDefinition() {
        return columnDefinition;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalCreateIndex(" + indexName + ")\n";
    }
}
