package ru.open.cu.student.parser.nodes;

public class CreateIndexStmt implements AstNode {
    public final String indexName;
    public final String tableName;
    public final String columnName;
    public final String indexType; // optional, e.g. HASH/BTREE

    public CreateIndexStmt(String indexName, String tableName, String columnName, String indexType) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.indexType = indexType;
    }
}
