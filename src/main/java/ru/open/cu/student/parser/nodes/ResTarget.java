package ru.open.cu.student.parser.nodes;

public class ResTarget implements AstNode {
    public final Expr val;    // обычно ColumnRef а будущем может быть выражение
    public final String name; // элиес (может быть нул)

    public ResTarget(Expr val, String name) {
        this.val = val;
        this.name = name;
    }
}
