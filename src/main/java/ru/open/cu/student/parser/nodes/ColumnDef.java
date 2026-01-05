package ru.open.cu.student.parser.nodes;

public class ColumnDef implements AstNode {
    public final String name;
    public final String typeName;

    public ColumnDef(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }
}
