package ru.open.cu.student.lexer;

public class Token {
    public String type; // SELECT, FROM, IDENT, NUMBER, COMMA, SEMICOLON, …
    public String value; // оригинальное значение, например "users"

    public Token() {
    }

    public Token(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Token{" + type + ":'" + value + "'}";
    }
}
