package ru.open.cu.student.execution;

import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.operation.OperationManager;
import ru.open.cu.student.execution.executors.*;
import ru.open.cu.student.index.Index;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.index.btree.BPlusTreeIndex;
import ru.open.cu.student.index.hash.HashIndex;
import ru.open.cu.student.optimizer.node.*;

import java.util.List;
import java.util.function.Predicate;

public class ExecutorFactoryImpl implements ExecutorFactory {

    private final CatalogManager catalogManager;
    private final OperationManager operationManager;
    private final IndexRegistry indexRegistry;

    public ExecutorFactoryImpl(CatalogManager catalogManager,
                               OperationManager operationManager,
                               IndexRegistry indexRegistry) {
        this.catalogManager = catalogManager;
        this.operationManager = operationManager;
        this.indexRegistry = indexRegistry;
    }

    @Override
    public Executor createExecutor(PhysicalPlanNode plan) {
        return build(plan);
    }

    private Executor build(PhysicalPlanNode plan) {
        if (plan instanceof ru.open.cu.student.optimizer.node.PhysicalCreateNode create) {
            return new ru.open.cu.student.execution.executors.CreateTableExecutor(
                    catalogManager,
                    create.getTableDefinition(),
                    create.getColumns()
            );
        } else if (plan instanceof ru.open.cu.student.optimizer.node.PhysicalCreateIndexNode createIndex) {
            return new CreateIndexExecutor(
                    operationManager,
                    indexRegistry,
                    createIndex.getIndexName(),
                    createIndex.getTableDefinition(),
                    createIndex.getColumnDefinition(),
                    createIndex.getIndexType()
            );
        } else if (plan instanceof ru.open.cu.student.optimizer.node.PhysicalResetNode reset) {
            return new ResetExecutor(operationManager, reset.getScope());
        } else if (plan instanceof PhysicalInsertNode insert) {
            return new InsertExecutor(operationManager, insert.getTableDefinition(), insert.getValues());

        } else if (plan instanceof PhysicalIndexScanNode indexNode) {
            Index index = indexRegistry.getByName(indexNode.getIndexName());
            if (index == null) {
                throw new IllegalArgumentException("Index not found: " + indexNode.getIndexName());
            }
            if (indexNode.getIndexType() == IndexType.HASH) {
                return new HashIndexScanExecutor(
                        operationManager,
                        (HashIndex) index,
                        indexNode.getTableDefinition().name,
                        indexNode.getPredicate().getValue()
                );
            } else {
                return new BTreeIndexScanExecutor(
                        operationManager,
                        (BPlusTreeIndex) index,
                        indexNode.getTableDefinition().name,
                        indexNode.getPredicate()
                );
            }

        } else if (plan instanceof PhysicalSeqScanNode scan) {
            return new SeqScanExecutor(operationManager, scan.getTable(), scan.getOutputColumns());

        } else if (plan instanceof PhysicalFilterNode filter) {
            Executor childExec = build(filter.getChild());
            // Компилируем предикат в рантайм-предикат над строкой
            java.util.function.Predicate<List<Object>> pred =
                    ExecutorsSupport.compilePredicate(filter.getPredicate(), filter.getChild().getOutputColumns());
            return new FilterExecutor(childExec, pred);

        } else if (plan instanceof PhysicalProjectNode project) {
            Executor childExec = build(project.getChild());
            List<ExecutorsSupport.ProjectItem> items = ExecutorsSupport.compileProjectItems(
                    project.getTargetList(), project.getChild().getOutputColumns()
            );
            return new ProjectExecutor(childExec, items);
        }

        throw new UnsupportedOperationException(
                "Unsupported physical plan node: " + plan.getClass().getSimpleName()
        );
    }
}
