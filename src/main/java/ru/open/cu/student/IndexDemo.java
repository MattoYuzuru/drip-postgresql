package ru.open.cu.student;

import ru.open.cu.student.catalog.manager.CatalogManagerImpl;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.catalog.operation.OperationManagerImpl;
import ru.open.cu.student.execution.executors.BTreeIndexScanExecutor;
import ru.open.cu.student.execution.executors.Executor;
import ru.open.cu.student.execution.executors.HashIndexScanExecutor;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.index.btree.BPlusTreeIndex;
import ru.open.cu.student.index.btree.BPlusTreeIndexImpl;
import ru.open.cu.student.index.hash.HashIndex;
import ru.open.cu.student.index.hash.HashIndexImpl;
import ru.open.cu.student.planner.node.IndexPredicate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Small demo showcasing hash and B+-tree indexes.
 */
public class IndexDemo {

    public static void main(String[] args) throws IOException {
        Path tempDir = Files.createTempDirectory("index-demo");
        System.out.println("Demo data stored in " + tempDir);

        var catalog = new CatalogManagerImpl(tempDir);
        var indexRegistry = new IndexRegistry();
        var operationManager = new OperationManagerImpl(catalog, null, indexRegistry);

        TypeDefinition intType = catalog.registerType("INT8", 8);
        TypeDefinition varcharType = catalog.registerType("VARCHAR", -1);

        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition(null, null, intType.oid, "id", 0));
        columns.add(new ColumnDefinition(null, null, varcharType.oid, "name", 1));
        columns.add(new ColumnDefinition(null, null, intType.oid, "age", 2));
        TableDefinition table = catalog.createTable("users", columns);

        HashIndex hashIndex = new HashIndexImpl("idx_users_id", "users", "id");
        BPlusTreeIndex btreeIndex = new BPlusTreeIndexImpl("idx_users_age", "users", "age");
        indexRegistry.register(hashIndex);
        indexRegistry.register(btreeIndex);

        operationManager.insert("users", List.of(1L, "Alice", 30L));
        operationManager.insert("users", List.of(2L, "Bob", 25L));
        operationManager.insert("users", List.of(3L, "Carol", 40L));
        operationManager.insert("users", List.of(4L, "Dave", 35L));

        System.out.println("Hash search id=2 -> " + hashIndex.search(2L));
        System.out.println("B+Tree range [25, 40] -> " + btreeIndex.rangeSearch(25L, true, 40L, true));

        Executor hashExec = new HashIndexScanExecutor(operationManager, hashIndex, table.name, 2L);
        hashExec.open();
        Object row;
        System.out.println("HashIndexScan rows:");
        while ((row = hashExec.next()) != null) {
            System.out.println(row);
        }
        hashExec.close();

        Executor btreeExec = new BTreeIndexScanExecutor(operationManager, btreeIndex, table.name,
                IndexPredicate.range(25L, true, 40L, true));
        btreeExec.open();
        System.out.println("BTreeIndexScan rows:");
        while ((row = btreeExec.next()) != null) {
            System.out.println(row);
        }
        btreeExec.close();
    }
}
