package ru.open.cu.student.parser.nodes;

import java.util.List;

public class SelectStmt implements AstNode {
    public final List<ResTarget> targetList;
    public final List<RangeVar> fromClause; // сейчас 1 таблица, список для расширения
    public final Expr whereClause; // может быть нул

    public SelectStmt(List<ResTarget> targetList, List<RangeVar> fromClause, Expr whereClause) {
        this.targetList = targetList;
        this.fromClause = fromClause;
        this.whereClause = whereClause;
    }
}
