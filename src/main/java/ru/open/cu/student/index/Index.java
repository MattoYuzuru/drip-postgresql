package ru.open.cu.student.index;

public interface Index {

    void insert(Comparable<?> key, TID tid);

    String getName();

    IndexType getType();

    String getTableName();

    String getColumnName();
}
