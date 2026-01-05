package ru.open.cu.student.semantic;

import ru.open.cu.student.ast.QueryType;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.index.IndexType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QueryTree {
    public QueryType commandType;
    public String tableName; // общая точка для create/insert/index
    public final List<TableDefinition> fromTables = new ArrayList<>();
    public final List<QColumn> targetList = new ArrayList<>();
    public SExpr filter; // может быть нул
    public final List<CreateColumn> createColumns = new ArrayList<>();
    public final List<SExpr> insertValues = new ArrayList<>();
    public String indexName;
    public ColumnDefinition indexColumn;
    public IndexType indexType;

    public static class QColumn {
        public final TableDefinition table;
        public final ColumnDefinition column;
        public final TypeDefinition type;   // получаем через catalog.getTypeByOid(column.typeOid)

        public QColumn(TableDefinition table, ColumnDefinition column, TypeDefinition type) {
            this.table = table;
            this.column = column;
            this.type = type;
        }

        public String normalizedType() {
            if (type == null || type.name == null) return "UNKNOWN";
            String n = type.name.toUpperCase();
            if (n.startsWith("INT")) return "INT";
            if (n.contains("CHAR") || n.startsWith("VARCHAR") || n.contains("TEXT")) return "STRING";
            return n;
        }

        @Override
        public String toString() {
            return "Column(\"" + table.name + "." + column.name + "\": " + normalizedType() + ")";
        }
    }

    public static class CreateColumn {
        public final String name;
        public final TypeDefinition type;

        public CreateColumn(String name, TypeDefinition type) {
            this.name = name;
            this.type = type;
        }
    }

    // ==== Семантические выражения ====
    public interface SExpr { }

    public static class SConst implements SExpr {
        public final String literal; // как в лексере (например 18)
        public final String type;    // INT/STRING/UNKNOWN

        public SConst(String literal, String type) {
            this.literal = literal;
            this.type = type;
        }

        @Override
        public String toString() {
            if (Objects.equals(type, "STRING")) {
                return "Const('" + literal + "': " + type + ")";
            }
            return "Const(" + literal + ": " + type + ")";
        }
    }

    public static class SColumn implements SExpr {
        public final QColumn column;

        public SColumn(QColumn column) {
            this.column = column;
        }

        @Override
        public String toString() {
            return column.toString();
        }
    }

    public static class SAExpr implements SExpr {
        public final String op; // >, >=, <, <=, =, <>
        public final SExpr left;
        public final SExpr right;

        public SAExpr(String op, SExpr left, SExpr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "AExpr(\"" + op + "\", " + left + ", " + right + ")";
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QueryTree\n");
        sb.append("├── commandType: ").append(commandType).append("\n");
        if (tableName != null) {
            sb.append("├── tableName: ").append(tableName).append("\n");
        }

        sb.append("├── fromTables: [");
        for (int i = 0; i < fromTables.size(); i++) {
            TableDefinition t = fromTables.get(i);
            sb.append("Table(\"").append(t.name).append("\")");
            if (i < fromTables.size() - 1) sb.append(", ");
        }
        sb.append("]\n");

        sb.append("├── targetList: [");
        for (int i = 0; i < targetList.size(); i++) {
            sb.append(targetList.get(i));
            if (i < targetList.size() - 1) sb.append(", ");
        }
        sb.append("]\n");

        sb.append("└── filter: ");
        sb.append(filter == null ? "null" : filter.toString());
        sb.append("\n");

        if (!createColumns.isEmpty()) {
            sb.append("└── createColumns: [");
            for (int i = 0; i < createColumns.size(); i++) {
                CreateColumn c = createColumns.get(i);
                sb.append(c.name).append(":").append(c.type != null ? c.type.name : "UNKNOWN");
                if (i < createColumns.size() - 1) sb.append(", ");
            }
            sb.append("]\n");
        }
        if (!insertValues.isEmpty()) {
            sb.append("└── insertValues: [");
            for (int i = 0; i < insertValues.size(); i++) {
                sb.append(insertValues.get(i));
                if (i < insertValues.size() - 1) sb.append(", ");
            }
            sb.append("]\n");
        }
        if (indexName != null) {
            sb.append("└── index: ").append(indexName);
            if (indexColumn != null) {
                sb.append(" on ").append(indexColumn.name);
            }
            if (indexType != null) {
                sb.append(" (").append(indexType).append(")");
            }
        }

        return sb.toString();
    }
}
