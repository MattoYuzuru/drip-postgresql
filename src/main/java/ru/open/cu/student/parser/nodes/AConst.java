package ru.open.cu.student.parser.nodes;

public class AConst implements Expr {
    public final String literal; // храним текст, типизацию сделает семантика

    public AConst(String literal) {
        this.literal = literal;
    }
}
