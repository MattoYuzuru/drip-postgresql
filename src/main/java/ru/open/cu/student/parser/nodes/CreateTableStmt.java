package ru.open.cu.student.parser.nodes;

import java.util.List;

public class CreateTableStmt implements AstNode {
    public final String tableName;
    public final List<ColumnDef> columns;

    public CreateTableStmt(String tableName, List<ColumnDef> columns) {
        this.tableName = tableName;
        this.columns = List.copyOf(columns);
    }
}
