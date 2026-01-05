package ru.open.cu.student.ast;


public class TargetEntry {

    public Expr expr;          // выражение для вычисления
    public String alias;       // псевдоним результата
    public String resultType;  // тип результата

    public TargetEntry(Expr expr, String alias) {
        this.expr = expr;
        this.alias = alias;
    }
}
