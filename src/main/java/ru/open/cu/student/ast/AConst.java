package ru.open.cu.student.ast;

public class AConst extends Expr {
    public Object value;       // значение: число, строка, boolean, null

    public AConst(Object val) {
        this.value = val;
    }

    @Override
    public String toString() {
        if (value == null) return "NULL";
        if (value instanceof String s) return '\'' + s.replace("'", "''") + '\'';
        return String.valueOf(value);
    }
}