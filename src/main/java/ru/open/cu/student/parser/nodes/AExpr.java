package ru.open.cu.student.parser.nodes;

public class AExpr implements Expr {
    public final String op; // ">", ">=", "<", "<=", "=", "<>"
    public final Expr left;
    public final Expr right;

    public AExpr(String op, Expr left, Expr right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }
}
