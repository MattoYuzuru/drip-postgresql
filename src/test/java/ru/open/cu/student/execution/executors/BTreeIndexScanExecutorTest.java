package ru.open.cu.student.execution.executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.catalog.manager.CatalogManagerImpl;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.catalog.operation.OperationManagerImpl;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.index.btree.BPlusTreeIndex;
import ru.open.cu.student.index.btree.BPlusTreeIndexImpl;
import ru.open.cu.student.planner.node.IndexPredicate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BTreeIndexScanExecutorTest {

    @Test
    void rangeExecutorReturnsOrderedRows(@TempDir Path tempDir) throws IOException {
        CatalogManagerImpl catalog = new CatalogManagerImpl(tempDir);
        IndexRegistry registry = new IndexRegistry();
        OperationManagerImpl op = new OperationManagerImpl(catalog, null, registry);

        TypeDefinition intType = catalog.registerType("INT8", 8);
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition(null, null, intType.oid, "value", 0));
        TableDefinition table = catalog.createTable("nums", columns);

        BPlusTreeIndex index = new BPlusTreeIndexImpl("idx_nums_value", table.name, "value");
        registry.register(index);

        op.insert(table.name, List.of(5L));
        op.insert(table.name, List.of(10L));
        op.insert(table.name, List.of(15L));

        Executor executor = new BTreeIndexScanExecutor(op, index, table.name,
                IndexPredicate.range(5L, true, 12L, false));
        executor.open();
        assertEquals(List.of(5L), executor.next());
        assertEquals(List.of(10L), executor.next());
        assertNull(executor.next());
        executor.close();
    }
}
