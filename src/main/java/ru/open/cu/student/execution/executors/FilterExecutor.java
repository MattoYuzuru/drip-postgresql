package ru.open.cu.student.execution.executors;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Исполнитель фильтрации WHERE.
 * Работает поверх дочернего Executor и отбирает строки по предикату.
 */
public class FilterExecutor implements Executor {

    private final Executor child;
    private final Predicate<List<Object>> predicate;

    public FilterExecutor(Executor child, Predicate<List<Object>> predicate) {
        this.child = child;
        this.predicate = predicate;
    }

    @Override
    public void open() {
        child.open();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object next() throws IOException {
        Object row;
        while ((row = child.next()) != null) {
            List<Object> asList = (List<Object>) row;
            if (predicate == null || predicate.test(asList)) {
                return asList;
            }
        }
        return null;
    }

    @Override
    public void close() {
        child.close();
    }
}