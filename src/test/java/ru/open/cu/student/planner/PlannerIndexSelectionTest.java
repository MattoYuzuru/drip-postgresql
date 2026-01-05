package ru.open.cu.student.planner;

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
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.index.hash.HashIndexImpl;
import ru.open.cu.student.planner.Planner;
import ru.open.cu.student.planner.node.IndexScanNode;
import ru.open.cu.student.planner.node.LogicalPlanNode;
import ru.open.cu.student.planner.node.ProjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerIndexSelectionTest {

    @TempDir
    Path tempDir;

    private CatalogManagerImpl catalog;
    private IndexRegistry registry;
    private Planner planner;

    @BeforeEach
    void setUp() throws IOException {
        catalog = new CatalogManagerImpl(tempDir);
        registry = new IndexRegistry();
        planner = new PlannerImpl(catalog, registry);

        TypeDefinition intType = catalog.registerType("INT8", 8);
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition(null, null, intType.oid, "id", 0));
        catalog.createTable("users", columns);

        registry.register(new HashIndexImpl("idx_users_id", "users", "id"));
    }

    @Test
    void usesIndexWhenAvailable() {
        QueryTree query = new QueryTree();
        query.commandType = QueryType.SELECT;
        query.rangeTable.add(new RangeTblEntry("users"));
        query.targetList.add(new TargetEntry(new ColumnRef("users", "id"), "id"));
        query.whereClause = new AExpr("=", new ColumnRef("users", "id"), new AConst(1L));

        LogicalPlanNode logical = planner.plan(query);
        assertTrue(logical instanceof ProjectNode);
        ProjectNode project = (ProjectNode) logical;
        assertTrue(project.getChild() instanceof IndexScanNode);
    }
}
