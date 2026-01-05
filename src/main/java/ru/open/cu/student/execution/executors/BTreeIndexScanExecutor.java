package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.operation.OperationManager;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.index.btree.BPlusTreeIndex;
import ru.open.cu.student.planner.node.IndexPredicate;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class BTreeIndexScanExecutor implements Executor {

    private final OperationManager operationManager;
    private final BPlusTreeIndex index;
    private final String tableName;
    private final IndexPredicate predicate;

    private Iterator<TID> iterator;
    private boolean opened;

    public BTreeIndexScanExecutor(OperationManager operationManager,
                                  BPlusTreeIndex index,
                                  String tableName,
                                  IndexPredicate predicate) {
        this.operationManager = operationManager;
        this.index = index;
        this.tableName = tableName;
        this.predicate = predicate;
    }

    @Override
    public void open() {
        if (opened) return;
        List<TID> tids = resolveTids();
        iterator = tids.iterator();
        opened = true;
    }

    private List<TID> resolveTids() {
        if (predicate == null) {
            return index.scanAll();
        }
        return switch (predicate.getType()) {
            case EQUAL -> index.search(predicate.getValue());
            case RANGE -> index.rangeSearch(
                    predicate.getRangeFrom(), predicate.isIncludeFrom(),
                    predicate.getRangeTo(), predicate.isIncludeTo()
            );
            case GREATER_OR_EQUAL -> index.searchGreaterThan(predicate.getValue(), true);
            case GREATER_THAN -> index.searchGreaterThan(predicate.getValue(), false);
            case LESS_OR_EQUAL -> index.searchLessThan(predicate.getValue(), true);
            case LESS_THAN -> index.searchLessThan(predicate.getValue(), false);
        };
    }

    @Override
    public Object next() throws IOException {
        if (!opened || iterator == null) return null;
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
