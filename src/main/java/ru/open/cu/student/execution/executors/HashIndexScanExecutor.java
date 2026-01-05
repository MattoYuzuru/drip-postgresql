package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.operation.OperationManager;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.index.hash.HashIndex;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class HashIndexScanExecutor implements Executor {

    private final OperationManager operationManager;
    private final HashIndex index;
    private final String tableName;
    private final Comparable<?> searchKey;

    private Iterator<TID> iterator;
    private boolean opened;

    public HashIndexScanExecutor(OperationManager operationManager,
                                 HashIndex index,
                                 String tableName,
                                 Comparable<?> searchKey) {
        this.operationManager = operationManager;
        this.index = index;
        this.tableName = tableName;
        this.searchKey = searchKey;
    }

    @Override
    public void open() {
        if (opened) return;
        List<TID> tids = searchKey == null ? List.of() : index.search(searchKey);
        iterator = tids.iterator();
        opened = true;
    }

    @Override
    public Object next() throws IOException {
        if (!opened || iterator == null) {
            return null;
        }
        while (iterator.hasNext()) {
            TID tid = iterator.next();
            List<Object> row = operationManager.fetchRow(tableName, tid);
            if (row != null) {
                return row;
            }
        }
        return null;
    }

    @Override
    public void close() {
        opened = false;
        iterator = null;
    }
}
