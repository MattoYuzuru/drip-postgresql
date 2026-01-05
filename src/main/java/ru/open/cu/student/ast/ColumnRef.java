package ru.open.cu.student.ast;

public class ColumnRef extends Expr {
    public String table;       // имя таблицы (может быть null)
    public String column;      // имя столбца

    public ColumnRef(String table, String column) {
        this.table = table;
        this.column = column;
    }

    public ColumnRef(String column) {
        this(null, column);  // Только имя столбца, таблица не указана
    }

    @Override
    public String toString() {
        return (table == null || table.isBlank()) ? column : (table + "." + column);
    }
}