package ru.open.cu.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.ast.AConst;
import ru.open.cu.student.ast.AExpr;
import ru.open.cu.student.ast.ColumnRef;
import ru.open.cu.student.ast.QueryTree;
import ru.open.cu.student.ast.QueryType;
import ru.open.cu.student.ast.RangeTblEntry;
import ru.open.cu.student.ast.TargetEntry;
import ru.open.cu.student.catalog.manager.CatalogManagerImpl;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.catalog.operation.OperationManagerImpl;
import ru.open.cu.student.execution.ExecutorFactory;
import ru.open.cu.student.execution.ExecutorFactoryImpl;
import ru.open.cu.student.execution.QueryExecutionEngine;
import ru.open.cu.student.execution.QueryExecutionEngineImpl;
import ru.open.cu.student.execution.executors.Executor;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.optimizer.Optimizer;
import ru.open.cu.student.optimizer.OptimizerImpl;
import ru.open.cu.student.optimizer.node.PhysicalPlanNode;
import ru.open.cu.student.planner.Planner;
import ru.open.cu.student.planner.PlannerImpl;
import ru.open.cu.student.planner.node.FilterNode;
import ru.open.cu.student.planner.node.LogicalPlanNode;
import ru.open.cu.student.planner.node.ProjectNode;
import ru.open.cu.student.planner.node.ScanNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlProcessorTest {

    @TempDir
    Path tempDir;

    private Planner planner;
    private Optimizer optimizer;
    private ExecutorFactory executorFactory;
    private QueryExecutionEngine executionEngine;

    @BeforeEach
    void setUp() throws IOException {
        CatalogManagerImpl catalog = new CatalogManagerImpl(tempDir);
        IndexRegistry indexRegistry = new IndexRegistry();

        TypeDefinition intType = catalog.registerType("INT8", 8);
        TypeDefinition varcharType = catalog.registerType("VARCHAR", -1);

        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition(null, null, intType.oid, "id", 0));
        columns.add(new ColumnDefinition(null, null, varcharType.oid, "name", 1));
        columns.add(new ColumnDefinition(null, null, intType.oid, "age", 2));

        catalog.createTable("users", columns);

        OperationManagerImpl operationManager = new OperationManagerImpl(catalog, null, indexRegistry);
        operationManager.insert("users", List.of(1L, "Alice", 30L));
        operationManager.insert("users", List.of(2L, "Bob", 17L));
        operationManager.insert("users", List.of(3L, "Carol", 21L));

        planner = new PlannerImpl(catalog, indexRegistry);
        optimizer = new OptimizerImpl();
        executorFactory = new ExecutorFactoryImpl(catalog, operationManager, indexRegistry);
        executionEngine = new QueryExecutionEngineImpl();
    }

    @Test
    void plannerBuildsProjectFilterScanChain() {
        LogicalPlanNode plan = planner.plan(buildSelectQuery());
        assertInstanceOf(ProjectNode.class, plan, "Root node must be Project");

        ProjectNode project = (ProjectNode) plan;
        assertNotNull(project.getChild());
        assertEquals(List.of("name"), project.getOutputColumns());

        assertInstanceOf(FilterNode.class, project.getChild(), "Project child must be Filter");
        FilterNode filter = (FilterNode) project.getChild();
        assertNotNull(filter.getChild());

        assertInstanceOf(ScanNode.class, filter.getChild(), "Filter child must be SeqScan");
        ScanNode scan = (ScanNode) filter.getChild();
        assertEquals(List.of("id", "name", "age"), scan.getOutputColumns());
    }

    @Test
    @SuppressWarnings("unchecked")
    void executorChainFiltersAndProjectsRows() {
        LogicalPlanNode logicalPlan = planner.plan(buildSelectQuery());
        PhysicalPlanNode physicalPlan = optimizer.optimize(logicalPlan);
        Executor executor = executorFactory.createExecutor(physicalPlan);

        List<Object> rows = executionEngine.execute(executor);
        List<List<Object>> data = rows.stream()
                .map(row -> (List<Object>) row)
                .toList();

        assertEquals(List.of(
                List.of("Alice"),
                List.of("Carol")
        ), data);
    }

    private QueryTree buildSelectQuery() {
        QueryTree query = new QueryTree();
        query.commandType = QueryType.SELECT;

        RangeTblEntry range = new RangeTblEntry("users");
        range.index = 0;
        query.rangeTable.add(range);

        TargetEntry target = new TargetEntry(new ColumnRef("users", "name"), "name");
        query.targetList.add(target);

        query.whereClause = new AExpr(
                ">",
                new ColumnRef("users", "age"),
                new AConst(18L)
        );

        return query;
    }
}
