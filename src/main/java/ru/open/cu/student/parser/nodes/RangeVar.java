package ru.open.cu.student.parser.nodes;

public class RangeVar implements AstNode {
    public final String relname;
    public final String alias; // может быть нул

    public RangeVar(String relname) {
        this(relname, null);
    }

    public RangeVar(String relname, String alias) {
        this.relname = relname;
        this.alias = alias;
    }
}
