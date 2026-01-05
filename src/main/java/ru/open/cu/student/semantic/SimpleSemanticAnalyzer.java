// ru/open/cu/student/semantic/SimpleSemanticAnalyzer.java
package ru.open.cu.student.semantic;

import ru.open.cu.student.ast.QueryType;
import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.parser.nodes.*;

import java.util.*;

public class SimpleSemanticAnalyzer implements SemanticAnalyzer {

    @Override
    public QueryTree analyze(AstNode ast, CatalogManager catalog) {
        if (ast instanceof SelectStmt select) {
            return analyzeSelect(select, catalog);
        } else if (ast instanceof CreateTableStmt create) {
            return analyzeCreateTable(create, catalog);
        } else if (ast instanceof InsertStmt insert) {
            return analyzeInsert(insert, catalog);
        } else if (ast instanceof CreateIndexStmt createIndex) {
            return analyzeCreateIndex(createIndex, catalog);
        } else if (ast instanceof ResetStmt reset) {
            return analyzeReset(reset, catalog);
        }
        throw fail("Неподдерживаемый AST: " + ast.getClass().getSimpleName());
    }

    private QueryTree analyzeSelect(SelectStmt select, CatalogManager catalog) {
        QueryTree qt = new QueryTree();
        qt.commandType = QueryType.SELECT;

        Map<String, TableDefinition> scope = new LinkedHashMap<>(); // имя/алиас -> таблица
        List<TableDefinition> fromTables = new ArrayList<>();
        for (RangeVar rv : select.fromClause) {
            TableDefinition t = requireTable(catalog, rv.relname);
            fromTables.add(t);
            // имя таблицы
            scope.put(rv.relname, t);
            // алиас
            if (rv.alias != null && !rv.alias.isEmpty()) {
                if (scope.containsKey(rv.alias)) {
                    throw fail("Дублирующий алиас таблицы: " + rv.alias);
                }
                scope.put(rv.alias, t);
            }
        }

        qt.fromTables.addAll(fromTables);

        for (ResTarget rt : select.targetList) {
            if (!(rt.val instanceof ColumnRef cr)) {
                throw fail("В нашем подмножестве SELECT поддерживает только простые ColumnRef");
            }
            if ("*".equals(cr.name)) {
                expandAllColumns(qt, scope, catalog);
                continue;
            }
            if (cr.name != null && cr.name.endsWith(".*")) {
                String qualifier = cr.name.substring(0, cr.name.length() - 2);
                expandTableColumns(qt, scope, catalog, qualifier);
                continue;
            }
            QueryTree.QColumn bound = resolveColumn(scope, catalog, cr.name);
            qt.targetList.add(bound);
        }

        if (select.whereClause != null) {
            qt.filter = bindExpr(scope, catalog, select.whereClause);
        }

        return qt;
    }

    private QueryTree analyzeCreateTable(CreateTableStmt create, CatalogManager catalog) {
        QueryTree qt = new QueryTree();
        qt.commandType = QueryType.CREATE;
        qt.tableName = create.tableName;

        if (!(catalog instanceof ru.open.cu.student.catalog.manager.CatalogManagerImpl impl)) {
            throw fail("CatalogManagerImpl required");
        }

        for (ColumnDef col : create.columns) {
            TypeDefinition type = resolveTypeDefinition(impl, col.typeName);
            qt.createColumns.add(new QueryTree.CreateColumn(col.name, type));
        }
        return qt;
    }

    private QueryTree analyzeInsert(InsertStmt insert, CatalogManager catalog) {
        QueryTree qt = new QueryTree();
        qt.commandType = QueryType.INSERT;
        qt.tableName = insert.tableName;

        TableDefinition table = requireTable(catalog, insert.tableName);
        qt.fromTables.add(table);
        List<ColumnDefinition> cols = ((ru.open.cu.student.catalog.manager.CatalogManagerImpl) catalog)
                .getColumnsForTable(table);
        if (cols.size() != insert.values.size()) {
            throw fail("Values size mismatch: expected " + cols.size() + " got " + insert.values.size());
        }
        Map<String, TableDefinition> scope = Map.of(insert.tableName, table);
        for (Expr expr : insert.values) {
            qt.insertValues.add(bindExpr(scope, catalog, expr));
        }
        return qt;
    }

    private QueryTree analyzeCreateIndex(CreateIndexStmt createIndex, CatalogManager catalog) {
        QueryTree qt = new QueryTree();
        qt.commandType = QueryType.CREATE_INDEX;
        qt.tableName = createIndex.tableName;
        qt.indexName = createIndex.indexName;

        TableDefinition table = requireTable(catalog, createIndex.tableName);
        ColumnDefinition col = catalog.getColumn(table, createIndex.columnName);
        if (col == null) {
            throw fail("Колонка не найдена: " + createIndex.columnName);
        }
        qt.indexColumn = col;
        qt.indexType = parseIndexType(createIndex.indexType);
        return qt;
    }

    private QueryTree analyzeReset(ResetStmt reset, CatalogManager catalog) {
        QueryTree qt = new QueryTree();
        qt.commandType = QueryType.RESET;
        qt.tableName = reset.scope;
        return qt;
    }

    private IndexType parseIndexType(String raw) {
        if (raw == null || raw.isEmpty()) return IndexType.BTREE;
        return switch (raw.toUpperCase()) {
            case "HASH" -> IndexType.HASH;
            case "BTREE" -> IndexType.BTREE;
            default -> throw fail("Unknown index type: " + raw);
        };
    }

    private QueryTree.SExpr bindExpr(Map<String, TableDefinition> scope,
                                     CatalogManager catalog,
                                     Expr expr) {
        switch (expr) {
            case ColumnRef cr -> {
                return new QueryTree.SColumn(resolveColumn(scope, catalog, cr.name));
            }
            case AConst c -> {
                return new QueryTree.SConst(c.literal, inferConstType(c.literal));
            }
            case AExpr a -> {
                QueryTree.SExpr l = bindExpr(scope, catalog, a.left);
                QueryTree.SExpr r = bindExpr(scope, catalog, a.right);
                checkComparable(a.op, l, r);
                return new QueryTree.SAExpr(a.op, l, r);
            }
            case null, default -> {
                assert expr != null;
                throw fail("Неподдерживаемый тип выражения: " + expr.getClass().getSimpleName());
            }
        }
    }

    private void expandAllColumns(QueryTree qt, Map<String, TableDefinition> scope, CatalogManager catalog) {
        for (TableDefinition t : new LinkedHashSet<>(scope.values())) {
            expandTableColumns(qt, scope, catalog, t.name);
        }
    }

    private void expandTableColumns(QueryTree qt,
                                    Map<String, TableDefinition> scope,
                                    CatalogManager catalog,
                                    String qualifier) {
        TableDefinition t = scope.get(qualifier);
        if (t == null) {
            throw fail("Таблица не найдена: " + qualifier);
        }
        List<ColumnDefinition> cols = ((ru.open.cu.student.catalog.manager.CatalogManagerImpl) catalog)
                .getColumnsForTable(t);
        cols.sort(Comparator.comparingInt(c -> c.position == null ? 0 : c.position));
        for (ColumnDefinition col : cols) {
            TypeDefinition type = catalog.getTypeByOid(col.typeOid);
            qt.targetList.add(new QueryTree.QColumn(t, col, type));
        }
    }

    private void checkComparable(String op, QueryTree.SExpr l, QueryTree.SExpr r) {
        String lt = exprType(l);
        String rt = exprType(r);
        if (!Objects.equals(lt, rt)) {
            throw fail("Несовместимые типы для " + op + ": " + lt + " " + op + " " + rt);
        }
    }

    private String exprType(QueryTree.SExpr e) {
        if (e instanceof QueryTree.SConst c) return c.type;
        if (e instanceof QueryTree.SColumn c) return c.column.normalizedType();
        return "UNKNOWN";
    }

    private String inferConstType(String lit) {
        if (lit == null) return "UNKNOWN";
        if ((lit.startsWith("'") && lit.endsWith("'")) ||
                (lit.startsWith("\"") && lit.endsWith("\""))) {
            return "STRING";
        }
        try {
            Long.parseLong(lit);
            return "INT";
        } catch (NumberFormatException ignored) {
        }
        return "STRING";
    }

    private QueryTree.QColumn resolveColumn(Map<String, TableDefinition> scope,
                                            CatalogManager catalog,
                                            String columnName) {
        String qualifier = null;
        String name = columnName;
        if (columnName != null && columnName.contains(".")) {
            String[] parts = columnName.split("\\.", 2);
            qualifier = parts[0];
            name = parts[1];
        }
        if (qualifier != null) {
            TableDefinition t = scope.get(qualifier);
            if (t == null) {
                throw fail("Таблица не найдена: " + qualifier);
            }
            ColumnDefinition col = catalog.getColumn(t, name);
            if (col == null) {
                throw fail("Колонка не найдена: " + columnName);
            }
            TypeDefinition type = catalog.getTypeByOid(col.typeOid);
            return new QueryTree.QColumn(t, col, type);
        }

        List<QueryTree.QColumn> matches = new ArrayList<>();
        for (TableDefinition t : new LinkedHashSet<>(scope.values())) {
            ColumnDefinition col = catalog.getColumn(t, name);
            if (col != null) {
                TypeDefinition type = catalog.getTypeByOid(col.typeOid);
                matches.add(new QueryTree.QColumn(t, col, type));
            }
        }
        if (matches.isEmpty()) {
            throw fail("Колонка не найдена: " + columnName);
        }
        if (matches.size() > 1) {
            String opts = String.join(", ",
                    matches.stream().map(c -> c.table.name + "." + c.column.name).toList());
            throw fail("Неоднозначная колонка '" + columnName + "': " + opts);
        }
        return matches.getFirst();
    }

    private TableDefinition requireTable(CatalogManager catalog, String table) {
        TableDefinition t = catalog.getTable(table);
        if (t == null) throw fail("Таблица не найдена: " + table);
        return t;
    }

    private RuntimeException fail(String msg) {
        return new RuntimeException("[Semantic] " + msg);
    }

    private TypeDefinition resolveTypeDefinition(ru.open.cu.student.catalog.manager.CatalogManagerImpl catalog,
                                                 String raw) {
        if (raw == null) {
            throw fail("Unknown type: null");
        }
        String trimmed = raw.trim();
        String upper = trimmed.toUpperCase();
        if (upper.startsWith("VARCHAR(") || upper.startsWith("CHAR(")) {
            int open = trimmed.indexOf('(');
            int close = trimmed.indexOf(')', open + 1);
            if (open < 0 || close < 0) {
                throw fail("Invalid type syntax: " + raw);
            }
            String lenRaw = trimmed.substring(open + 1, close).trim();
            int len;
            try {
                len = Integer.parseInt(lenRaw);
            } catch (NumberFormatException ex) {
                throw fail("Invalid length for type " + raw);
            }
            if (len <= 0) {
                throw fail("Invalid length for type " + raw);
            }
            String base = upper.startsWith("VARCHAR(") ? "VARCHAR" : "CHAR";
            String typeName = base + "(" + len + ")";
            TypeDefinition existing = catalog.getTypeByName(typeName);
            if (existing != null) return existing;
            try {
                return catalog.registerType(typeName, len);
            } catch (Exception e) {
                throw fail("Failed to register type: " + typeName);
            }
        }

        TypeDefinition type = catalog.getTypeByName(upper);
        if (type == null) {
            type = catalog.getTypeByName(trimmed);
        }
        if (type == null) {
            throw fail("Unknown type: " + raw);
        }
        return type;
    }
}
