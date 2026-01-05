package ru.open.cu.student.ast;

import ru.open.cu.student.index.IndexType;

import java.util.ArrayList;
import java.util.List;

public class QueryTree {
    public List<RangeTblEntry> rangeTable;  // таблицы, участвующие в запросе
    public List<TargetEntry> targetList;    // что выбираем
    public Expr whereClause;                // условие отбора
    public QueryType commandType;           // SELECT, INSERT, etc.
    public String indexName;                // для CREATE INDEX
    public String indexColumn;
    public IndexType indexType;


    public QueryTree() {
        this.rangeTable = new ArrayList<>();
        this.targetList = new ArrayList<>();
    }

}
