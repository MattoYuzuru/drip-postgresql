package ru.open.cu.student.execution.executors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Исполнитель SELECT-списка — выбирает нужные колонки/выражения из строк.
 * Работает поверх дочернего Executor.
 */
public class ProjectExecutor implements Executor {

    private final Executor child;
    private final List<ExecutorsSupport.ProjectItem> items;

    public ProjectExecutor(Executor child, List<ExecutorsSupport.ProjectItem> items) {
        this.child = child;
        this.items = List.copyOf(items);
    }

    @Override
    public void open() { child.open(); }

    @SuppressWarnings("unchecked")
    @Override
    public Object next() throws IOException {
        Object row = child.next();
        if (row == null) return null;
        List<Object> in = (List<Object>) row;
        List<Object> out = new ArrayList<>(items.size());
        for (ExecutorsSupport.ProjectItem it : items) {
            out.add(it.evaluate(in));
        }
        return out;
    }

    @Override
    public void close() { child.close(); }
}
