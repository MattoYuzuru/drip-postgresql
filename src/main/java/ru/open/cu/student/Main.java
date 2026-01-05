package ru.open.cu.student;

import ru.open.cu.student.catalog.manager.CatalogManagerImpl;
import ru.open.cu.student.catalog.operation.OperationManagerImpl;
import ru.open.cu.student.execution.ExecutorFactoryImpl;
import ru.open.cu.student.execution.QueryExecutionEngineImpl;
import ru.open.cu.student.execution.executors.Executor;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.lexer.SimpleLexer;
import ru.open.cu.student.memory.buffer.DefaultBufferPoolManager;
import ru.open.cu.student.memory.manager.HeapPageFileManager;
import ru.open.cu.student.memory.replacer.LRUReplacer;
import ru.open.cu.student.optimizer.OptimizerImpl;
import ru.open.cu.student.optimizer.node.PhysicalPlanNode;
import ru.open.cu.student.parser.SimpleParser;
import ru.open.cu.student.planner.PlannerImpl;
import ru.open.cu.student.planner.node.LogicalPlanNode;
import ru.open.cu.student.semantic.SimpleSemanticAnalyzer;

import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        var catalog = new CatalogManagerImpl();
        var buffer = new DefaultBufferPoolManager(
                8, new HeapPageFileManager(), new LRUReplacer()
        );
        var indexRegistry = new IndexRegistry();
        var opManager = new OperationManagerImpl(catalog, buffer, indexRegistry);

        var planner = new PlannerImpl(catalog, indexRegistry);
        var optimizer = new OptimizerImpl();
        var executors = new ExecutorFactoryImpl(catalog, opManager, indexRegistry);
        var engine = new QueryExecutionEngineImpl();
        var lexer = new SimpleLexer();
        var parser = new SimpleParser();
        var semantic = new SimpleSemanticAnalyzer();

        var input = new Scanner(System.in);
        var query = input.nextLine();
        var tokens = lexer.tokenize(query);
        var ast = parser.parse(tokens);
        var queryTreeSem = semantic.analyze(ast, catalog);
        var queryTree = ru.open.cu.student.bridge.SemanticToAstAdapter.toAst(queryTreeSem);
        LogicalPlanNode logical = planner.plan(queryTree);

        PhysicalPlanNode physical = optimizer.optimize(logical);
        Executor root = executors.createExecutor(physical);

        var result = engine.execute(root);
        for (Object row : result) {
            System.out.println(row);
        }
    }
}
