package ru.open.cu.student.parser.nodes;

public class ColumnRef implements Expr {
    public final String name;

    public ColumnRef(String name) {
        this.name = name;
    }
}
