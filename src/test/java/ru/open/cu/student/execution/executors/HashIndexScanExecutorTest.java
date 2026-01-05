package ru.open.cu.student.execution.executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.catalog.manager.CatalogManagerImpl;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.catalog.operation.OperationManagerImpl;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.index.hash.HashIndex;
import ru.open.cu.student.index.hash.HashIndexImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HashIndexScanExecutorTest {

    @Test
    void returnsIndexedRows(@TempDir Path tempDir) throws IOException {
        CatalogManagerImpl catalog = new CatalogManagerImpl(tempDir);
        IndexRegistry registry = new IndexRegistry();
        OperationManagerImpl op = new OperationManagerImpl(catalog, null, registry);

        TypeDefinition intType = catalog.registerType("INT8", 8);
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition(null, null, intType.oid, "id", 0));
        TableDefinition table = catalog.createTable("numbers", columns);

        HashIndex hashIndex = new HashIndexImpl("idx_numbers_id", table.name, "id");
        registry.register(hashIndex);

        op.insert(table.name, List.of(1L));
        op.insert(table.name, List.of(2L));

        Executor executor = new HashIndexScanExecutor(op, hashIndex, table.name, 2L);
        executor.open();
        Object row = executor.next();
        assertEquals(List.of(2L), row);
        assertNull(executor.next());
        executor.close();
    }
}
