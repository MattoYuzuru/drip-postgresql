package ru.open.cu.student.index;

import java.util.*;

public class IndexRegistry {

    private final Map<String, Index> byName = new HashMap<>();
    private final Map<String, Map<String, List<Index>>> byTableColumn = new HashMap<>();

    public synchronized void register(Index index) {
        Objects.requireNonNull(index, "index");
        if (byName.containsKey(index.getName())) {
            throw new IllegalArgumentException("Index already registered: " + index.getName());
        }
        byName.put(index.getName(), index);
        byTableColumn
                .computeIfAbsent(index.getTableName(), table -> new HashMap<>())
                .computeIfAbsent(index.getColumnName(), column -> new ArrayList<>())
                .add(index);
    }

    public synchronized Index getByName(String name) {
        return byName.get(name);
    }

    public synchronized List<Index> getByTable(String tableName) {
        Map<String, List<Index>> byColumn = byTableColumn.get(tableName);
        if (byColumn == null) return List.of();
        return byColumn.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public synchronized List<Index> getByTableAndColumn(String tableName, String columnName) {
        Map<String, List<Index>> byColumn = byTableColumn.get(tableName);
        if (byColumn == null) return List.of();
        return byColumn.getOrDefault(columnName, List.of());
    }

    public synchronized void clear() {
        byName.clear();
        byTableColumn.clear();
    }
}
