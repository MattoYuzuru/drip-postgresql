package ru.open.cu.student.planner;

import ru.open.cu.student.ast.*;
import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.Index;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.planner.node.*;

import java.util.List;

public class PlannerImpl implements Planner {

    private final CatalogManager catalogManager;
    private final IndexRegistry indexRegistry;

    public PlannerImpl(CatalogManager catalogManager, IndexRegistry indexRegistry) {
        this.catalogManager = catalogManager;
        this.indexRegistry = indexRegistry;
    }

    @Override
    public LogicalPlanNode plan(QueryTree queryTree) {
        if (queryTree == null) throw new IllegalArgumentException("QueryTree is null");

        return switch (queryTree.commandType) {
            case CREATE -> planCreate(queryTree);
            case CREATE_INDEX -> planCreateIndex(queryTree);
            case RESET -> planReset(queryTree);
            case INSERT -> planInsert(queryTree);
            case SELECT -> planSelect(queryTree);
        };
    }

    // ---------- CREATE ----------
    private LogicalPlanNode planCreate(QueryTree q) {
        String tableName = extractTableName(q);


        java.util.List<ru.open.cu.student.catalog.model.ColumnDefinition> columns = new java.util.ArrayList<>();
        int position = 0;
        for (ru.open.cu.student.ast.TargetEntry te : q.targetList) {


            var type = ((ru.open.cu.student.catalog.manager.CatalogManagerImpl) catalogManager).getTypeByName(te.resultType);
            if (type == null) throw new IllegalArgumentException("Unknown type: " + te.resultType);
            columns.add(new ru.open.cu.student.catalog.model.ColumnDefinition(
                    null,
                    null,
                    type.oid,
                    te.alias,
                    position++
            ));
        }

        var tableDef = new ru.open.cu.student.catalog.model.TableDefinition(0, tableName, "USER", tableName, 0);

        return new ru.open.cu.student.planner.node.CreateTableNode(tableDef, columns);
    }

    // ---------- INSERT ----------
    private LogicalPlanNode planInsert(QueryTree q) {
        String tableName = extractTableName(q);
        TableDefinition tableDef = catalogManager.getTable(tableName);

        List<Expr> values = q.targetList.stream()
                .map(te -> te.expr)
                .toList();

        return new InsertNode(tableDef, values);
    }

    // ---------- SELECT (Project -> Filter? -> Scan) ----------
    private LogicalPlanNode planSelect(QueryTree q) {
        String tableName = extractTableName(q);
        var tableDef = catalogManager.getTable(tableName);
        if (tableDef == null) {
            throw new IllegalArgumentException("Unknown table: " + tableName);
        }

        // Схема скана из каталога (имена колонок по position)
        var cols = ((ru.open.cu.student.catalog.manager.CatalogManagerImpl) catalogManager)
                .getColumnsForTable(tableDef);

        // Сортировка по позиции (Integer), защита от null
        cols.sort(java.util.Comparator.comparingInt(c -> c.position != null ? c.position : 0));
        var fullSchema = new java.util.ArrayList<String>(cols.size());
        for (var c : cols) fullSchema.add(c.name);

        LogicalPlanNode current;
        IndexSelection indexSelection = trySelectIndex(tableDef, q.whereClause);
        if (indexSelection != null) {
            current = new IndexScanNode(
                    tableDef,
                    fullSchema,
                    indexSelection.index().getName(),
                    indexSelection.index().getType(),
                    indexSelection.predicate()
            );
        } else {
            current = new ScanNode(tableDef, fullSchema);
            if (q.whereClause != null) {
                current = new FilterNode(q.whereClause, current);
            }
        }

        if (q.targetList == null || q.targetList.isEmpty()) {
            throw new IllegalArgumentException("Empty SELECT target list");
        }
        return new ProjectNode(q.targetList, current);
    }

    private LogicalPlanNode planCreateIndex(QueryTree q) {
        String tableName = extractTableName(q);
        TableDefinition tableDef = catalogManager.getTable(tableName);
        if (tableDef == null) {
            throw new IllegalArgumentException("Unknown table: " + tableName);
        }
        String columnName = q.indexColumn;
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("Index column not specified");
        }
        var columnDef = ((ru.open.cu.student.catalog.manager.CatalogManagerImpl) catalogManager)
                .getColumn(tableDef, columnName);
        if (columnDef == null) {
            throw new IllegalArgumentException("Unknown column for index: " + columnName);
        }
        String indexName = (q.indexName == null || q.indexName.isBlank())
                ? "idx_" + tableName + "_" + columnName
                : q.indexName;
        IndexType indexType = q.indexType == null ? IndexType.BTREE : q.indexType;
        return new CreateIndexNode(indexName, tableDef, columnDef, indexType);
    }

    private LogicalPlanNode planReset(QueryTree q) {
        String scope = null;
        if (q.rangeTable != null && !q.rangeTable.isEmpty()) {
            scope = q.rangeTable.getFirst().tableName;
        }
        return new ResetNode(scope);
    }

    private String extractTableName(QueryTree q) {
        if (q.rangeTable != null && !q.rangeTable.isEmpty()) {
            q.rangeTable.getFirst();
            return q.rangeTable.getFirst().tableName;
        }
        throw new IllegalArgumentException("Cannot determine table name");
    }

    private IndexSelection trySelectIndex(TableDefinition tableDefinition, Expr whereClause) {
        if (whereClause == null || indexRegistry == null) {
            return null;
        }
        if (!(whereClause instanceof AExpr aExpr)) {
            return null;
        }
        ColumnRef columnRef = extractColumnRef(aExpr.left);
        AConst constant = extractConst(aExpr.right);
        String operator = aExpr.op;
        if (columnRef == null || constant == null) {
            columnRef = extractColumnRef(aExpr.right);
            constant = extractConst(aExpr.left);
            operator = flipOperator(operator);
        }
        if (columnRef == null || constant == null || operator == null) {
            return null;
        }
        Comparable<?> value = asComparable(constant.value);
        if (value == null) return null;
        String columnName = columnRef.column;
        List<Index> indexes = indexRegistry.getByTableAndColumn(tableDefinition.name, columnName);
        if (indexes.isEmpty()) {
            return null;
        }
        IndexPredicate predicate = buildPredicate(operator, value);
        if (predicate == null) return null;
        Index chosen = chooseIndex(indexes, predicate.getType());
        if (chosen == null) return null;
        return new IndexSelection(chosen, predicate);
    }

    private Index chooseIndex(List<Index> indexes, IndexPredicate.Type predicateType) {
        if (predicateType == IndexPredicate.Type.EQUAL) {
            return indexes.stream()
                    .filter(i -> i.getType() == IndexType.HASH)
                    .findFirst()
                    .orElseGet(() -> indexes.stream()
                            .filter(i -> i.getType() == IndexType.BTREE)
                            .findFirst()
                            .orElse(null));
        }
        return indexes.stream()
                .filter(i -> i.getType() == IndexType.BTREE)
                .findFirst()
                .orElse(null);
    }

    private ColumnRef extractColumnRef(Expr expr) {
        if (expr instanceof ColumnRef columnRef) {
            return columnRef;
        }
        return null;
    }

    private AConst extractConst(Expr expr) {
        if (expr instanceof AConst c) {
            return c;
        }
        return null;
    }

    private Comparable<?> asComparable(Object value) {
        if (value instanceof Comparable<?> cmp) {
            return cmp;
        }
        return null;
    }

    private String flipOperator(String op) {
        if (op == null) return null;
        return switch (op) {
            case ">" -> "<";
            case ">=" -> "<=";
            case "<" -> ">";
            case "<=" -> ">=";
            default -> op;
        };
    }

    private IndexPredicate buildPredicate(String operator, Comparable<?> value) {
        return switch (operator) {
            case "=", "==" -> IndexPredicate.equal(value);
            case ">" -> IndexPredicate.greaterThan(value, false);
            case ">=" -> IndexPredicate.greaterThan(value, true);
            case "<" -> IndexPredicate.lessThan(value, false);
            case "<=" -> IndexPredicate.lessThan(value, true);
            default -> null;
        };
    }

    private record IndexSelection(Index index, IndexPredicate predicate) {
    }
}
