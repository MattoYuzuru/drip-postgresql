package ru.open.cu.student.catalog.operation;

import ru.open.cu.student.catalog.manager.CatalogManagerImpl;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.index.Index;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.memory.buffer.BufferPoolManager;
import ru.open.cu.student.memory.model.BufferSlot;
import ru.open.cu.student.memory.page.HeapPage;
import ru.open.cu.student.memory.page.Page;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperationManagerImpl implements OperationManager {

    private final CatalogManagerImpl catalog;
    private final BufferPoolManager bufferManager;
    private final IndexRegistry indexRegistry;
    private final Map<String, List<List<Object>>> tableRows = new HashMap<>();
    private final Map<String, Map<String, Integer>> columnPositionsCache = new HashMap<>();

    public OperationManagerImpl(CatalogManagerImpl catalog,
                                BufferPoolManager bufferManager,
                                IndexRegistry indexRegistry) {
        this.catalog = catalog;
        this.bufferManager = bufferManager;
        this.indexRegistry = indexRegistry;
    }

    public OperationManagerImpl(CatalogManagerImpl catalog, BufferPoolManager bufferManager) {
        this(catalog, bufferManager, new IndexRegistry());
    }

    @Override
    public void insert(String tableName, List<Object> values) {
        TableDefinition table = catalog.getTable(tableName);
        if (table == null) throw new IllegalArgumentException("Table not found: " + tableName);

        List<ColumnDefinition> cols = catalog.getColumnsForTable(table);
        if (cols.size() != values.size()) {
            throw new IllegalArgumentException("Values size mismatch: expected " + cols.size() + " got " + values.size());
        }

        if (bufferManager != null) {
            insertWithBufferManager(table, cols, values);
        } else {
            insertDirectToFile(table, cols, values);
        }

        List<Object> snapshot = List.copyOf(values);
        TID tid = persistRow(tableName, snapshot);
        updateIndexes(table, snapshot, tid);
    }

    private void insertWithBufferManager(TableDefinition table, List<ColumnDefinition> cols, List<Object> values) {
        try {
            byte[] rowBytes = serializeRow(cols, values);

            // Находим страницу с достаточным местом или создаем новую
            int pageId = findPageWithSpace(table, rowBytes.length);
            if (pageId == -1) {
                pageId = createNewPage(table);
            }

            // Получаем страницу через буферный менеджер
            BufferSlot slot = bufferManager.getPage(pageId);
            Page page = slot.getPage();

            if (!(page instanceof HeapPage heapPage)) {
                throw new IllegalStateException("Expected HeapPage, got " + page.getClass().getSimpleName());
            }

            heapPage.write(rowBytes);

            // Обновляем страницу в буфере
            bufferManager.updatePage(pageId, heapPage);
            bufferManager.flushPage(pageId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to insert row", e);
        }
    }

    private void insertDirectToFile(TableDefinition table, List<ColumnDefinition> cols, List<Object> values) {
        // Старая реализация для обратной совместимости
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(table.fileNode, true))) {
            DataOutputStream dos = new DataOutputStream(os);
            for (int i = 0; i < cols.size(); i++) {
                ColumnDefinition col = cols.get(i);
                TypeDefinition t = catalog.getTypeByOid(col.typeOid);
                Object val = values.get(i);
                if (t == null) throw new IllegalStateException("Type not found: " + col.typeOid);

                if (t.byteLength != null && t.byteLength > 0) {
                    if (t.byteLength == 8) {
                        long v;
                        if (val instanceof Number) v = ((Number) val).longValue();
                        else v = Long.parseLong(val.toString());
                        dos.writeLong(v);
                    } else {
                        byte[] raw = val.toString().getBytes(StandardCharsets.UTF_8);
                        if (raw.length > t.byteLength) {
                            throw new IllegalArgumentException("Value too large for column " + col.name);
                        }
                        dos.write(raw);
                        dos.write(new byte[t.byteLength - raw.length]);
                    }
                } else {
                    byte[] raw = val == null ? new byte[0] : val.toString().getBytes(StandardCharsets.UTF_8);
                    dos.writeInt(raw.length);
                    dos.write(raw);
                }
            }
            dos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] serializeRow(List<ColumnDefinition> cols, List<Object> values) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        for (int i = 0; i < cols.size(); i++) {
            ColumnDefinition col = cols.get(i);
            TypeDefinition t = catalog.getTypeByOid(col.typeOid);
            Object val = values.get(i);
            if (t == null) throw new IllegalStateException("Type not found: " + col.typeOid);

            if (t.byteLength != null && t.byteLength > 0) {
                if (t.byteLength == 8) {
                    long v;
                    if (val instanceof Number) v = ((Number) val).longValue();
                    else v = Long.parseLong(val.toString());
                    dos.writeLong(v);
                } else {
                    byte[] raw = val.toString().getBytes(StandardCharsets.UTF_8);
                    if (raw.length > t.byteLength) {
                        throw new IllegalArgumentException("Value too large for column " + col.name);
                    }
                    dos.write(raw);
                    dos.write(new byte[t.byteLength - raw.length]);
                }
            } else {
                byte[] raw = val == null ? new byte[0] : val.toString().getBytes(StandardCharsets.UTF_8);
                dos.writeInt(raw.length);
                dos.write(raw);
            }
        }

        return bos.toByteArray();
    }

    private int findPageWithSpace(TableDefinition table, int requiredSpace) {
        // Простая реализация - проверяем существующие страницы
        for (int pageId = 0; pageId < table.pagesCount; pageId++) {
            try {
                BufferSlot slot = bufferManager.getPage(pageId);
                Page page = slot.getPage();

                if (page instanceof HeapPage heapPage) {
                    int free = freeSpace(heapPage);
                    if (free >= requiredSpace + 4) {
                        return pageId;
                    }
                }
            } catch (Exception e) {
                // Страница недоступна, пропускаем
            }
        }
        return -1;
    }

    private int createNewPage(TableDefinition table) throws IOException {
        int newPageId = table.pagesCount;

        // Создаем новую страницу
        HeapPage newPage = new HeapPage(newPageId);

        // Записываем её через буферный менеджер
        bufferManager.getPage(newPageId);
        bufferManager.updatePage(newPageId, newPage);
        bufferManager.flushPage(newPageId);

        // Обновляем количество страниц в каталоге
        table.pagesCount++;
        catalog.updateTablePagesCount(table.name, table.pagesCount);

        return newPageId;
    }

    private int freeSpace(HeapPage page) {
        byte[] bytes = page.bytes();
        int lower = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
        int upper = ((bytes[8] & 0xFF) << 8) | (bytes[9] & 0xFF);
        return upper - lower;
    }

    @Override
    public List<Object> select(String tableName, List<String> columnNames) {
        TableDefinition table = catalog.getTable(tableName);
        if (table == null) throw new IllegalArgumentException("Table not found: " + tableName);

        List<ColumnDefinition> cols = catalog.getColumnsForTable(table);

        // Находим индексы запрашиваемых колонок
        List<Integer> requestedIdx = new ArrayList<>();
        for (String colName : columnNames) {
            boolean found = false;
            for (int i = 0; i < cols.size(); i++) {
                if (cols.get(i).name.equals(colName)) {
                    requestedIdx.add(i);
                    found = true;
                    break;
                }
            }
            if (!found) throw new IllegalArgumentException("Column not found: " + colName);
        }

        if (bufferManager != null) {
            return selectWithBufferManager(table, cols, requestedIdx);
        } else {
            return selectDirectFromFile(table, cols, requestedIdx);
        }
    }

    private List<Object> selectWithBufferManager(TableDefinition table, List<ColumnDefinition> cols, List<Integer> requestedIdx) {
        List<List<Object>> resultRows = new ArrayList<>();

        try {
            // Сканируем все страницы таблицы
            for (int pageId = 0; pageId < table.pagesCount; pageId++) {
                try {
                    BufferSlot slot = bufferManager.getPage(pageId);
                    Page page = slot.getPage();

                    if (page instanceof HeapPage heapPage) {

                        // Читаем все записи со страницы
                        for (int recordIndex = 0; recordIndex < heapPage.size(); recordIndex++) {
                            byte[] recordBytes = heapPage.read(recordIndex);
                            List<Object> row = deserializeRow(recordBytes, cols);

                            // Проецируем нужные колонки
                            List<Object> projected = new ArrayList<>();
                            for (int idx : requestedIdx) {
                                projected.add(row.get(idx));
                            }
                            resultRows.add(projected);
                        }
                    }
                } catch (Exception e) {
                    // Страница недоступна или повреждена, пропускаем
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to select data", e);
        }

        return new ArrayList<>(resultRows);
    }

    @Override
    public List<Object> fetchRow(String tableName, TID tid) {
        List<List<Object>> rows = tableRows.get(tableName);
        if (rows == null) {
            return null;
        }
        int rowIndex = tid.getPageId();
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return new ArrayList<>(rows.get(rowIndex));
    }

    @Override
    public void buildIndex(Index index) {
        if (index == null) return;
        List<List<Object>> rows = tableRows.get(index.getTableName());
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, Integer> positions = columnPositionsCache.computeIfAbsent(index.getTableName(), t -> {
            Map<String, Integer> map = new HashMap<>();
            TableDefinition table = catalog.getTable(t);
            if (table != null) {
                for (ColumnDefinition column : catalog.getColumnsForTable(table)) {
                    map.put(column.name, column.position);
                }
            }
            return map;
        });
        Integer pos = positions.get(index.getColumnName());
        if (pos == null) {
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (pos < row.size()) {
                Object value = row.get(pos);
                if (value instanceof Comparable<?> cmp) {
                    index.insert(cmp, TID.ofRowIndex(i));
                }
            }
        }
    }

    @Override
    public void reset(String scope) {
        if (scope != null && !scope.isBlank()) {
            throw new UnsupportedOperationException("Сброс отдельной таблицы пока не поддержан");
        }
        if (bufferManager instanceof ru.open.cu.student.memory.buffer.DefaultBufferPoolManager bpm) {
            bpm.flushAllPages();
            Path dbFile = bpm.getDbFilePath();
            try {
                Files.deleteIfExists(dbFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete data file: " + dbFile, e);
            }
            bpm.reset();
        }
        if (indexRegistry != null) {
            indexRegistry.clear();
        }
        tableRows.clear();
        columnPositionsCache.clear();
        if (catalog instanceof ru.open.cu.student.catalog.manager.CatalogManagerImpl impl) {
            try {
                impl.reset();
            } catch (IOException e) {
                throw new RuntimeException("Failed to reset catalog", e);
            }
        }
    }

    private List<Object> selectDirectFromFile(TableDefinition table, List<ColumnDefinition> cols, List<Integer> requestedIdx) {
        // Старая реализация для обратной совместимости
        List<List<Object>> resultRows = new ArrayList<>();

        try (InputStream is = new BufferedInputStream(new FileInputStream(table.fileNode))) {
            DataInputStream dis = new DataInputStream(is);
            while (dis.available() > 0) {
                List<Object> wholeRow = new ArrayList<>();

                ideaOptimization(cols, wholeRow, dis);

                List<Object> projected = new ArrayList<>();
                for (int idx : requestedIdx) {
                    projected.add(wholeRow.get(idx));
                }
                resultRows.add(projected);
            }
        } catch (EOFException eof) {
            // EOF - ok
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>(resultRows);
    }

    private List<Object> deserializeRow(byte[] recordBytes, List<ColumnDefinition> cols) throws IOException {
        List<Object> row = new ArrayList<>();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(recordBytes));

        ideaOptimization(cols, row, dis);

        return row;
    }

    private void ideaOptimization(List<ColumnDefinition> cols, List<Object> row, DataInputStream dis) throws IOException {
        for (ColumnDefinition col : cols) {
            TypeDefinition t = catalog.getTypeByOid(col.typeOid);
            if (t == null) throw new IllegalStateException("Type not found: " + col.typeOid);

            if (t.byteLength != null && t.byteLength > 0) {
                if (t.byteLength == 8) {
                    long v = dis.readLong();
                    row.add(v);
                } else {
                    byte[] buf = new byte[t.byteLength];
                    dis.readFully(buf);
                    int len = buf.length;
                    while (len > 0 && buf[len - 1] == 0) len--;
                    row.add(new String(buf, 0, len, StandardCharsets.UTF_8));
                }
            } else {
                int len = dis.readInt();
                byte[] buf = new byte[len];
                dis.readFully(buf);
                row.add(new String(buf, StandardCharsets.UTF_8));
            }
        }
    }

    private TID persistRow(String tableName, List<Object> snapshot) {
        List<List<Object>> rows = tableRows.computeIfAbsent(tableName, k -> new ArrayList<>());
        int rowIndex = rows.size();
        rows.add(snapshot);
        return TID.ofRowIndex(rowIndex);
    }

    private void updateIndexes(TableDefinition table, List<Object> rowValues, TID tid) {
        if (indexRegistry == null) return;
        List<Index> indexes = indexRegistry.getByTable(table.name);
        if (indexes.isEmpty()) return;
        Map<String, Integer> positions = columnPositionsCache.computeIfAbsent(table.name, t -> {
            Map<String, Integer> map = new HashMap<>();
            for (ColumnDefinition column : catalog.getColumnsForTable(table)) {
                map.put(column.name, column.position);
            }
            return map;
        });
        for (Index index : indexes) {
            Integer pos = positions.get(index.getColumnName());
            if (pos == null || pos < 0 || pos >= rowValues.size()) continue;
            Object value = rowValues.get(pos);
            if (value instanceof Comparable<?> cmp) {
                index.insert(cmp, tid);
            }
        }
    }
}
