package ru.open.cu.student.catalog.manager;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class CatalogManagerImpl implements CatalogManager {

    private final Path baseDir;
    private final Path tablesFile;
    private final Path columnsFile;
    private final Path typesFile;

    private final Map<Integer, TableDefinition> tablesByOid = new HashMap<>();
    private final Map<String, TableDefinition> tablesByName = new HashMap<>();
    private final Map<Integer, List<ColumnDefinition>> columnsByTableOid = new HashMap<>();
    private final Map<Integer, TypeDefinition> typesByOid = new HashMap<>();

    private final AtomicInteger nextTableOid = new AtomicInteger(1);
    private final AtomicInteger nextColumnOid = new AtomicInteger(1);
    private final AtomicInteger nextTypeOid = new AtomicInteger(1);

    public CatalogManagerImpl() {
        this(Path.of("."));
    }

    public CatalogManagerImpl(Path baseDir) {
        try {
            Path resolvedBase = baseDir == null ? Path.of(".") : baseDir;
            this.baseDir = resolvedBase.toAbsolutePath();
            Files.createDirectories(this.baseDir);
            this.tablesFile = this.baseDir.resolve("table_definitions.dat");
            this.columnsFile = this.baseDir.resolve("column_definitions.dat");
            this.typesFile = this.baseDir.resolve("types_definitions.dat");
            loadAll();
            ensureDefaultTypes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureDefaultTypes() throws IOException {
        if (getTypeByName("INT8") == null) {
            registerType("INT8", 8);
        }
        if (getTypeByName("VARCHAR") == null) {
            registerType("VARCHAR", -1);
        }
    }

    private void loadAll() throws IOException {
        if (Files.exists(typesFile)) {
            try (InputStream in = Files.newInputStream(typesFile)) {
                readRecords(in, bytes -> {
                    TypeDefinition td = TypeDefinition.fromBytes(bytes);
                    typesByOid.put(td.oid, td);
                    nextTypeOid.updateAndGet(v -> Math.max(v, td.oid + 1));
                });
            }
        }
        if (Files.exists(tablesFile)) {
            try (InputStream in = Files.newInputStream(tablesFile)) {
                readRecords(in, bytes -> {
                    TableDefinition td = TableDefinition.fromBytes(bytes);
                    tablesByOid.put(td.oid, td);
                    tablesByName.put(td.name, td);
                    nextTableOid.updateAndGet(v -> Math.max(v, td.oid + 1));
                });
            }
        }
        if (Files.exists(columnsFile)) {
            try (InputStream in = Files.newInputStream(columnsFile)) {
                readRecords(in, bytes -> {
                    ColumnDefinition cd = ColumnDefinition.fromBytes(bytes);
                    columnsByTableOid.computeIfAbsent(cd.tableOid, ignored -> new ArrayList<>()).add(cd);
                    nextColumnOid.updateAndGet(v -> Math.max(v, cd.oid + 1));
                });
            }
        }
    }

    private interface Reader {
        void accept(byte[] bytes) throws IOException;
    }

    private void readRecords(InputStream in, Reader r) throws IOException {
        DataInputStream din = new DataInputStream(in);
        while (true) {
            try {
                int len = din.readInt();
                byte[] buf = new byte[len];
                din.readFully(buf);
                r.accept(buf);
            } catch (EOFException ex) {
                break;
            }
        }
    }

    private synchronized void appendRecord(Path file, byte[] bytes) {
        try (OutputStream out = Files.newOutputStream(file, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
            DataOutputStream dout = new DataOutputStream(out);
            dout.writeInt(bytes.length);
            dout.write(bytes);
            dout.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized TableDefinition createTable(String name, List<ColumnDefinition> columns) throws IOException {
        if (tablesByName.containsKey(name)) {
            throw new IllegalArgumentException("Table already exists: " + name);
        }

        int tableOid = nextTableOid.getAndIncrement();
        String fileNode = baseDir.resolve(tableOid + ".dat").toString();
        TableDefinition table = new TableDefinition(tableOid, name, "TABLE", fileNode, 0);

        appendRecord(tablesFile, table.toBytes());
        tablesByOid.put(tableOid, table);
        tablesByName.put(name, table);

        List<ColumnDefinition> persistedCols = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition c = columns.get(i);
            int colOid = nextColumnOid.getAndIncrement();
            ColumnDefinition newCol = new ColumnDefinition(colOid, tableOid, c.typeOid, c.name, i);
            appendRecord(columnsFile, newCol.toBytes());
            persistedCols.add(newCol);
        }
        columnsByTableOid.put(tableOid, persistedCols);

        try {
            Files.createFile(Path.of(fileNode));
        } catch (IOException e) {
            if (!Files.exists(Path.of(fileNode))) {
                throw new RuntimeException(e);
            }
        }

        return table;
    }

    @Override
    public synchronized TableDefinition getTable(String tableName) {
        TableDefinition t = tablesByName.get(tableName);
        if (t == null) return null;
        return new TableDefinition(t.oid, t.name, t.type, t.fileNode, t.pagesCount);
    }

    @Override
    public synchronized ColumnDefinition getColumn(TableDefinition table, String columnName) {
        if (table == null) throw new IllegalArgumentException("table is null");
        List<ColumnDefinition> cols = columnsByTableOid.getOrDefault(table.oid, List.of());
        for (ColumnDefinition c : cols) {
            if (c.name.equals(columnName)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public synchronized List<TableDefinition> listTables() {
        return tablesByOid.values().stream()
                .map(t -> new TableDefinition(t.oid, t.name, t.type, t.fileNode, t.pagesCount))
                .collect(Collectors.toList());
    }

    @Override
    public TypeDefinition getTypeByOid(Integer typeOid) {
        return typesByOid.get(typeOid);
    }

    public synchronized List<ColumnDefinition> getColumnsForTable(TableDefinition table) {
        return columnsByTableOid.getOrDefault(table.oid, List.of());
    }

    public synchronized TypeDefinition registerType(String name, int byteLength) throws IOException {
        int oid = nextTypeOid.getAndIncrement();
        TypeDefinition t = new TypeDefinition(oid, name, byteLength);
        appendRecord(typesFile, t.toBytes());
        typesByOid.put(oid, t);
        return t;
    }

    public synchronized void updateTablePagesCount(String tableName, int newPagesCount) throws IOException {
        TableDefinition table = tablesByName.get(tableName);
        if (table != null) {
            table.pagesCount = newPagesCount;
            rewriteTablesFile();
        }
    }

    public synchronized TypeDefinition getTypeByName(String name) {
        if (name == null) return null;
        for (TypeDefinition t : typesByOid.values()) {
            if (name.equals(t.name) || (t.name != null && t.name.equalsIgnoreCase(name))) {
                return t;
            }
        }
        return null;
    }


    private void rewriteTablesFile() throws IOException {
        Files.deleteIfExists(tablesFile);
        for (TableDefinition table : tablesByOid.values()) {
            appendRecord(tablesFile, table.toBytes());
        }
    }

    public synchronized void reset() throws IOException {
        for (TableDefinition table : tablesByOid.values()) {
            if (table.fileNode != null) {
                Files.deleteIfExists(Path.of(table.fileNode));
            }
        }
        Files.deleteIfExists(tablesFile);
        Files.deleteIfExists(columnsFile);
        Files.deleteIfExists(typesFile);

        tablesByOid.clear();
        tablesByName.clear();
        columnsByTableOid.clear();
        typesByOid.clear();

        nextTableOid.set(1);
        nextColumnOid.set(1);
        nextTypeOid.set(1);

        ensureDefaultTypes();
    }
}
