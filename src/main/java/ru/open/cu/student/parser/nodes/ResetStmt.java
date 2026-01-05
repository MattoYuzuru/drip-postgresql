package ru.open.cu.student.parser.nodes;

public class ResetStmt implements AstNode {
    public final String scope;

    public ResetStmt(String scope) {
        this.scope = scope;
    }
}
