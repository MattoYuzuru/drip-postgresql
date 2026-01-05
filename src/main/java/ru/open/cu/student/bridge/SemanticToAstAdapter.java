package ru.open.cu.student.bridge;

import ru.open.cu.student.ast.*;
import ru.open.cu.student.ast.QueryType;
import ru.open.cu.student.semantic.QueryTree.SExpr;
import ru.open.cu.student.semantic.QueryTree.SConst;
import ru.open.cu.student.semantic.QueryTree.SColumn;
import ru.open.cu.student.semantic.QueryTree.SAExpr;

public final class SemanticToAstAdapter {
    private SemanticToAstAdapter() {
    }

    public static ru.open.cu.student.ast.QueryTree toAst(ru.open.cu.student.semantic.QueryTree s) {
        if (s == null) return null;
        return switch (s.commandType) {
            case SELECT -> toAstSelect(s);
            case CREATE -> toAstCreate(s);
            case INSERT -> toAstInsert(s);
            case CREATE_INDEX -> toAstCreateIndex(s);
            case RESET -> toAstReset(s);
        };
    }

    private static ru.open.cu.student.ast.QueryTree toAstSelect(ru.open.cu.student.semantic.QueryTree s) {
        ru.open.cu.student.ast.QueryTree q = new ru.open.cu.student.ast.QueryTree();
        q.commandType = QueryType.SELECT;

        // FROM (берем имена таблиц)
        for (int i = 0; i < s.fromTables.size(); i++) {
            var t = s.fromTables.get(i);
            var rte = new RangeTblEntry(t.name);
            rte.index = i;
            q.rangeTable.add(rte);
        }

        // SELECT список (каждый QColumn -> TargetEntry(ColumnRef))
        for (var qc : s.targetList) {
            var cref = new ColumnRef(qc.table.name, qc.column.name);
            var te = new TargetEntry(cref, qc.column.name);
            te.resultType = qc.type != null ? qc.type.name : "UNKNOWN";
            q.targetList.add(te);
        }

        // WHERE
        q.whereClause = convertExpr(s.filter);
        return q;
    }

    private static ru.open.cu.student.ast.QueryTree toAstCreate(ru.open.cu.student.semantic.QueryTree s) {
        ru.open.cu.student.ast.QueryTree q = new ru.open.cu.student.ast.QueryTree();
        q.commandType = QueryType.CREATE;
        q.rangeTable.add(new RangeTblEntry(s.tableName));
        for (var col : s.createColumns) {
            var te = new TargetEntry(new AConst(null), col.name);
            te.resultType = col.type.name;
            q.targetList.add(te);
        }
        return q;
    }

    private static ru.open.cu.student.ast.QueryTree toAstInsert(ru.open.cu.student.semantic.QueryTree s) {
        ru.open.cu.student.ast.QueryTree q = new ru.open.cu.student.ast.QueryTree();
        q.commandType = QueryType.INSERT;
        q.rangeTable.add(new RangeTblEntry(s.tableName));
        for (SExpr val : s.insertValues) {
            var te = new TargetEntry(convertExpr(val), null);
            q.targetList.add(te);
        }
        return q;
    }

    private static ru.open.cu.student.ast.QueryTree toAstCreateIndex(ru.open.cu.student.semantic.QueryTree s) {
        ru.open.cu.student.ast.QueryTree q = new ru.open.cu.student.ast.QueryTree();
        q.commandType = QueryType.CREATE_INDEX;
        q.rangeTable.add(new RangeTblEntry(s.tableName));
        q.indexName = s.indexName;
        q.indexColumn = s.indexColumn.name;
        q.indexType = s.indexType;
        return q;
    }

    private static ru.open.cu.student.ast.QueryTree toAstReset(ru.open.cu.student.semantic.QueryTree s) {
        ru.open.cu.student.ast.QueryTree q = new ru.open.cu.student.ast.QueryTree();
        q.commandType = QueryType.RESET;
        if (s.tableName != null) {
            q.rangeTable.add(new RangeTblEntry(s.tableName));
        }
        return q;
    }

    private static Expr convertExpr(SExpr e) {
        switch (e) {
            case null -> {
                return null;
            }
            case SConst c -> {
                // простая типизация констант
                Object val = switch (safeUpper(c.type)) {
                    case "INT", "INTEGER", "BIGINT", "SMALLINT" -> parseNumber(c.literal);
                    case "STRING", "TEXT", "VARCHAR", "CHAR" -> c.literal;
                    case "BOOL", "BOOLEAN" -> parseBoolean(c.literal);
                    default -> c.literal; // UNKNOWN — как строка
                };
                return new AConst(val);
            }
            case SColumn sc -> {
                return new ColumnRef(sc.column.table.name, sc.column.column.name);
            }
            case SAExpr ax -> {
                return new AExpr(ax.op, convertExpr(ax.left), convertExpr(ax.right));
            }
            default -> {
            }
        }
        throw new IllegalArgumentException("Unsupported SExpr: " + e.getClass().getSimpleName());
    }

    private static String safeUpper(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    private static Object parseNumber(String lit) {
        try {
            return Long.parseLong(lit);
        } catch (NumberFormatException nfe) {
            try {
                return Double.parseDouble(lit);
            } catch (NumberFormatException nfe2) {
                return lit; // fallback
            }
        }
    }

    private static Object parseBoolean(String lit) {
        if (lit == null) return false;
        return lit.equalsIgnoreCase("true") || lit.equals("1");
    }
}
