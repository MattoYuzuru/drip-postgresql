package ru.open.cu.student.catalog.operation;

import ru.open.cu.student.index.TID;
import ru.open.cu.student.index.Index;

import java.util.List;

public interface OperationManager {

    void insert(String tableName, List<Object> values);

    List<Object> select(String tableName, List<String> columnNames);

    List<Object> fetchRow(String tableName, TID tid);

    void buildIndex(Index index);

    void reset(String scope);
}
