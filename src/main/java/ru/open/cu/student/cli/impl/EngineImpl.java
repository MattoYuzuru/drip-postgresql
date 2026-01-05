package ru.open.cu.student.cli.impl;

import ru.open.cu.student.bridge.SemanticToAstAdapter;
import ru.open.cu.student.catalog.manager.CatalogManagerImpl;
import ru.open.cu.student.catalog.operation.OperationManagerImpl;
import ru.open.cu.student.cli.api.Engine;
import ru.open.cu.student.execution.ExecutorFactoryImpl;
import ru.open.cu.student.execution.QueryExecutionEngineImpl;
import ru.open.cu.student.execution.executors.Executor;
import ru.open.cu.student.index.IndexRegistry;
import ru.open.cu.student.lexer.Lexer;
import ru.open.cu.student.lexer.SimpleLexer;
import ru.open.cu.student.lexer.Token;
import ru.open.cu.student.memory.buffer.DefaultBufferPoolManager;
import ru.open.cu.student.memory.buffer.BufferPoolManager;
import ru.open.cu.student.memory.manager.HeapPageFileManager;
import ru.open.cu.student.memory.replacer.LRUReplacer;
import ru.open.cu.student.optimizer.OptimizerImpl;
import ru.open.cu.student.optimizer.node.PhysicalPlanNode;
import ru.open.cu.student.parser.Parser;
import ru.open.cu.student.parser.PrettyPrinter;
import ru.open.cu.student.parser.SimpleParser;
import ru.open.cu.student.parser.nodes.AstNode;
import ru.open.cu.student.planner.PlannerImpl;
import ru.open.cu.student.planner.node.LogicalPlanNode;
import ru.open.cu.student.semantic.SemanticAnalyzer;
import ru.open.cu.student.semantic.SimpleSemanticAnalyzer;

import java.nio.file.Path;
import java.util.List;

public class EngineImpl implements Engine {

    private final CatalogManagerImpl catalog;
    private final BufferPoolManager buffer;
    private final IndexRegistry indexRegistry;
    private final OperationManagerImpl operationManager;
    private final PlannerImpl planner;
    private final OptimizerImpl optimizer;
    private final ExecutorFactoryImpl executorFactory;
    private final QueryExecutionEngineImpl executionEngine;
    private final Lexer lexer;
    private final Parser parser;
    private final SemanticAnalyzer semanticAnalyzer;

    public EngineImpl() {
        this(Path.of("."));
    }

    public EngineImpl(Path baseDir) {
        this.catalog = new CatalogManagerImpl(baseDir);
        this.buffer = new DefaultBufferPoolManager(
                8, new HeapPageFileManager(), new LRUReplacer(), baseDir.resolve("data.db")
        );
        this.indexRegistry = new IndexRegistry();
        this.operationManager = new OperationManagerImpl(catalog, buffer, indexRegistry);
        this.planner = new PlannerImpl(catalog, indexRegistry);
        this.optimizer = new OptimizerImpl();
        this.executorFactory = new ExecutorFactoryImpl(catalog, operationManager, indexRegistry);
        this.executionEngine = new QueryExecutionEngineImpl();
        this.lexer = new SimpleLexer();
        this.parser = new SimpleParser();
        this.semanticAnalyzer = new SimpleSemanticAnalyzer();
    }

    @Override
    public synchronized String executeSql(String line) {
        try {
            List<Token> tokens = lexer.tokenize(line);
            logStage("Lexer", tokens.toString());

            AstNode ast = parser.parse(tokens);
            logStage("Parser", PrettyPrinter.print(ast).trim());

            ru.open.cu.student.semantic.QueryTree semanticTree = semanticAnalyzer.analyze(ast, catalog);
            logStage("Semantic", semanticTree.toString());

            ru.open.cu.student.ast.QueryTree queryTree = SemanticToAstAdapter.toAst(semanticTree);
            LogicalPlanNode logical = planner.plan(queryTree);
            logStage("Planner", logical.prettyPrint(""));

            PhysicalPlanNode physical = optimizer.optimize(logical);
            logStage("Optimizer", physical.prettyPrint(""));

            Executor executor = executorFactory.createExecutor(physical);
            List<Object> rows = executionEngine.execute(executor);
            logStage("Executor", "rows=" + rows.size());

            return formatResult(semanticTree.commandType, rows);
        } catch (Exception e) {
            logStage("Error", e.toString());
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return "ERROR: " + msg + "\nПоддерживаемые команды: смотрите howto.md";
        }
    }

    private String formatResult(ru.open.cu.student.ast.QueryType type, List<Object> rows) {
        if (type == ru.open.cu.student.ast.QueryType.SELECT) {
            StringBuilder sb = new StringBuilder();
            for (Object row : rows) {
                sb.append(row).append("\n");
            }
            sb.append("Rows: ").append(rows.size());
            return sb.toString();
        }
        if (type == ru.open.cu.student.ast.QueryType.RESET) {
            return "RESET OK";
        }
        return "OK";
    }

    private void logStage(String stage, String payload) {
        String thread = Thread.currentThread().getName();
        System.out.println("[" + thread + "] " + stage + ": " + payload);
    }
}
