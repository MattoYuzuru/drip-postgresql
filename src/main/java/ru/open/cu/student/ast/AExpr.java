package ru.open.cu.student.ast;

public class AExpr extends Expr {
    public final String op;   // ">", ">=", "<", "<=", "=", "<>"
    public final Expr left;
    public final Expr right;

    public AExpr(String op, Expr left, Expr right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "AExpr(\"" + op + "\", " + left + ", " + right + ")";
    }
}
