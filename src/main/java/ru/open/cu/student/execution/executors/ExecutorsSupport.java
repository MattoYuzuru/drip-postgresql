package ru.open.cu.student.execution.executors;

import ru.open.cu.student.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Утилиты для сборки рантайм-логики исполнителей из объектов плана.
 *
 * ВНИМАНИЕ: Это минимальная реализация для ДЗ. Поддерживаются только:
 *  - в Project: ColumnRef и константы (AConst)
 *  - в Filter: булева константа (AConst true/false)
 */
public final class ExecutorsSupport {
    private ExecutorsSupport() {}

    public sealed interface ProjectItem permits ConstItem, ColumnItem {
        Object evaluate(List<Object> inputRow);
    }
    public static final class ConstItem implements ProjectItem {
        private final Object value;
        public ConstItem(Object value) { this.value = value; }
        public Object evaluate(List<Object> inputRow) { return value; }
    }
    public static final class ColumnItem implements ProjectItem {
        private final int index;
        public ColumnItem(int index) { this.index = index; }
        public Object evaluate(List<Object> inputRow) { return inputRow.get(index); }
    }

    public static List<ProjectItem> compileProjectItems(List<TargetEntry> targets, List<String> inputSchema) {
        List<ProjectItem> items = new ArrayList<>();
        for (TargetEntry te : targets) {
            Expr e = te.expr;
            if (e instanceof AConst c) {
                items.add(new ConstItem(c.value));
            } else if (e instanceof ColumnRef cr) {
                String name = (cr.table == null || cr.table.isBlank()) ? cr.column : cr.table + "." + cr.column;
                int idx = findIndex(inputSchema, name);
                if (idx < 0) {
                    // пробуем без квалификатора
                    idx = findIndex(inputSchema, cr.column);
                }
                if (idx < 0) throw new IllegalArgumentException("Unknown column in projection: " + name);
                items.add(new ColumnItem(idx));
            } else {
                throw new UnsupportedOperationException("Only ColumnRef and AConst are supported in Project for now: " + e.getClass().getSimpleName());
            }
        }
        return items;
    }

    private static int findIndex(List<String> schema, String name) {
        for (int i = 0; i < schema.size(); i++) {
            if (Objects.equals(schema.get(i), name)) return i;
            // допускаем сравнение без учета регистра
            if (schema.get(i) != null && schema.get(i).equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    // FILTER
    public static Predicate<List<Object>> compilePredicate(Expr where, List<String> inputSchema) {
        if (where == null) return null;
        if (where instanceof AConst c) {
            boolean v = asBoolean(c.value);
            return row -> v;
        }
        ValueGetter getter = compileValueGetter(where, inputSchema);
        return row -> asBoolean(getter.eval(row));
    }

    private static boolean asBoolean(Object v) {
        return switch (v) {
            case null -> false;
            case Boolean b -> b;
            case Number n -> n.doubleValue() != 0.0;
            case String s -> !s.isEmpty() && !s.equalsIgnoreCase("false");
            default -> true;
        };
    }

    private interface ValueGetter { Object eval(List<Object> row); }

    private static ValueGetter compileValueGetter(Expr e, List<String> schema) {
        if (e instanceof AConst c) {
            return ignored -> c.value;
        } else if (e instanceof ColumnRef cr) {
            // ищем индекс колонки и по "table.col", и по "col"
            int idx = findIndex(schema, (cr.table == null || cr.table.isBlank()) ? cr.column : (cr.table + "." + cr.column));
            if (idx < 0) idx = findIndex(schema, cr.column);
            if (idx < 0) throw new IllegalArgumentException("Unknown column in predicate: " + cr);
            int finalIdx = idx;
            return row -> row.get(finalIdx);
        } else if (e instanceof AExpr nested) {
            ValueGetter l = compileValueGetter(nested.left, schema);
            ValueGetter r = compileValueGetter(nested.right, schema);
            String op = nested.op;
            return row -> compare(l.eval(row), r.eval(row), op); // вернёт Boolean
        }
        throw new UnsupportedOperationException("Unsupported value expr in predicate: " + e.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private static boolean compare(Object l, Object r, String op) {
        if (l == null || r == null) {
            return switch (op) {
                case "=", "==" -> Objects.equals(l, r);
                case "<>", "!=" -> !Objects.equals(l, r);
                default -> false;
            };
        }
        // числовая попытка
        Double ld = toNumberOrNull(l);
        Double rd = toNumberOrNull(r);
        if (ld != null && rd != null) {
            int cmp = Double.compare(ld, rd);
            return switch (op) {
                case "=","==" -> cmp == 0;
                case "<>","!=" -> cmp != 0;
                case ">" -> cmp > 0;
                case ">=" -> cmp >= 0;
                case "<" -> cmp < 0;
                case "<=" -> cmp <= 0;
                default -> throw new IllegalArgumentException("Unknown operator: " + op);
            };
        }
        // строковое сравнение
        String ls = String.valueOf(l);
        String rs = String.valueOf(r);
        int cmp = ls.compareTo(rs);
        return switch (op) {
            case "=","==" -> cmp == 0;
            case "<>","!=" -> cmp != 0;
            case ">" -> cmp > 0;
            case ">=" -> cmp >= 0;
            case "<" -> cmp < 0;
            case "<=" -> cmp <= 0;
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }

    private static Double toNumberOrNull(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception ignore) { return null; }
    }

}
