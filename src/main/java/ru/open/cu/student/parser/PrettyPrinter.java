package ru.open.cu.student.parser;

import ru.open.cu.student.parser.nodes.*;

import java.util.List;

public final class PrettyPrinter {
    private PrettyPrinter() {
    }

    public static String print(AstNode node) {
        StringBuilder sb = new StringBuilder();
        print(node, sb, "", true);
        return sb.toString();
    }

    private static void print(AstNode node, StringBuilder sb, String prefix, boolean isLast) {
        String branch = prefix + (isLast ? "└── " : "├── ");
        if (node instanceof SelectStmt s) {
            sb.append(prefix.isEmpty() ? "SelectStmt\n" : branch + "SelectStmt\n");
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            // targetList
            sb.append(childPrefix).append("├── targetList:\n");
            printList(s.targetList, sb, childPrefix + "│   ");
            // fromClause
            sb.append(childPrefix).append("├── fromClause:\n");
            printList(s.fromClause, sb, childPrefix + "│   ");
            // whereClause
            sb.append(childPrefix).append("└── whereClause: ");
            if (s.whereClause == null) {
                sb.append("null\n");
            } else {
                sb.append("\n");
                print(s.whereClause, sb, childPrefix + "    ", true);
            }
        } else if (node instanceof CreateTableStmt ct) {
            sb.append(prefix.isEmpty() ? "CreateTableStmt\n" : branch + "CreateTableStmt\n");
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            sb.append(childPrefix).append("├── table: ").append(ct.tableName).append("\n");
            sb.append(childPrefix).append("└── columns:\n");
            printList(ct.columns, sb, childPrefix + "    ");
        } else if (node instanceof ColumnDef cd) {
            sb.append(branch).append("ColumnDef(")
                    .append(cd.name).append(" ").append(cd.typeName)
                    .append(")\n");
        } else if (node instanceof CreateIndexStmt ci) {
            sb.append(prefix.isEmpty() ? "CreateIndexStmt\n" : branch + "CreateIndexStmt\n");
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            sb.append(childPrefix).append("├── index: ").append(ci.indexName).append("\n");
            sb.append(childPrefix).append("├── table: ").append(ci.tableName).append("\n");
            sb.append(childPrefix).append("├── column: ").append(ci.columnName).append("\n");
            sb.append(childPrefix).append("└── using: ").append(ci.indexType == null ? "BTREE" : ci.indexType).append("\n");
        } else if (node instanceof InsertStmt ins) {
            sb.append(prefix.isEmpty() ? "InsertStmt\n" : branch + "InsertStmt\n");
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            sb.append(childPrefix).append("├── table: ").append(ins.tableName).append("\n");
            sb.append(childPrefix).append("└── values:\n");
            printList(ins.values, sb, childPrefix + "    ");
        } else if (node instanceof ResetStmt rs) {
            sb.append(prefix.isEmpty() ? "ResetStmt\n" : branch + "ResetStmt\n");
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            sb.append(childPrefix).append("└── scope: ").append(rs.scope == null ? "ALL" : rs.scope).append("\n");
        } else if (node instanceof ResTarget rt) {
            sb.append(branch).append("ResTarget");
            if (rt.name != null) sb.append("(alias=").append(rt.name).append(")");
            sb.append("\n");
            print(rt.val, sb, prefix + (isLast ? "    " : "│   "), true);
        } else if (node instanceof RangeVar rv) {
            sb.append(branch).append("RangeVar(\"").append(rv.relname).append("\")\n");
        } else if (node instanceof ColumnRef cr) {
            sb.append(branch).append("ColumnRef(\"").append(cr.name).append("\")\n");
        } else if (node instanceof AConst c) {
            sb.append(branch).append("AConst(").append(c.literal).append(")\n");
        } else if (node instanceof AExpr e) {
            sb.append(branch).append("AExpr(\"").append(e.op).append("\")\n");
            String cp = prefix + (isLast ? "    " : "│   ");
            print(e.left, sb, cp, false);
            print(e.right, sb, cp, true);
        } else {
            sb.append(branch).append(node.getClass().getSimpleName()).append("\n");
        }
    }

    private static void printList(List<? extends AstNode> list, StringBuilder sb, String prefix) {
        for (int i = 0; i < list.size(); i++) {
            boolean last = (i == list.size() - 1);
            print(list.get(i), sb, prefix, last);
        }
        if (list.isEmpty()) {
            sb.append(prefix).append("(empty)\n");
        }
    }
}
