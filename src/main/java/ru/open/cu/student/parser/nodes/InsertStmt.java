package ru.open.cu.student.parser.nodes;

import java.util.List;

public class InsertStmt implements AstNode {
    public final String tableName;
    public final List<Expr> values;

    public InsertStmt(String tableName, List<Expr> values) {
        this.tableName = tableName;
        this.values = List.copyOf(values);
    }
}
