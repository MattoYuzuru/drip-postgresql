package ru.open.cu.student.lexer;

import java.util.ArrayList;
import java.util.List;

// input: SELECT name, age FROM users WHERE age > 18;
// output: [SELECT, IDENT(name), COMMA, IDENT(age), FROM, IDENT(users), WHERE, IDENT(age), GT, NUMBER(18), SEMICOLON]

/*
* Token
type: String (SELECT, FROM, IDENT, NUMBER, COMMA, SEMICOLON, …)
value: String (оригинальное значение, например "users")
*/

public class SimpleLexer implements Lexer {
    @Override
    public List<Token> tokenize(String sql) {
        var tokens = new ArrayList<Token>();
        if (sql == null) {
            return tokens;
        }

        int i = 0;
        while (i < sql.length()) {
            char ch = sql.charAt(i);
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }

            switch (ch) {
                case ',' -> {
                    tokens.add(new Token("COMMA", ","));
                    i++;
                    continue;
                }
                case '*' -> {
                    tokens.add(new Token("STAR", "*"));
                    i++;
                    continue;
                }
                case '.' -> {
                    tokens.add(new Token("DOT", "."));
                    i++;
                    continue;
                }
                case ';' -> {
                    tokens.add(new Token("SEMICOLON", ";"));
                    i++;
                    continue;
                }
                case '(' -> {
                    tokens.add(new Token("LPAREN", "("));
                    i++;
                    continue;
                }
                case ')' -> {
                    tokens.add(new Token("RPAREN", ")"));
                    i++;
                    continue;
                }
                case '\'', '"' -> {
                    char quote = ch;
                    int start = ++i;
                    while (i < sql.length() && sql.charAt(i) != quote) {
                        i++;
                    }
                    if (i >= sql.length()) {
                        throw new RuntimeException("Unterminated string literal");
                    }
                    String literal = sql.substring(start, i);
                    tokens.add(new Token("STRING", literal));
                    i++; // skip closing quote
                    continue;
                }
                default -> { }
            }

            // двухсимвольные/односивольные операторы сравнения
            if (isOperatorStart(ch)) {
                String op = String.valueOf(ch);
                if (i + 1 < sql.length()) {
                    char next = sql.charAt(i + 1);
                    if ((ch == '>' || ch == '<' || ch == '=') && next == '=') {
                        op = "" + ch + next;
                        i++;
                    } else if (ch == '<' && next == '>') {
                        op = "<>";
                        i++;
                    }
                }
                tokens.add(new Token("OP", op));
                i++;
                continue;
            }

            // число
            if (Character.isDigit(ch)) {
                int start = i;
                i++;
                while (i < sql.length() && Character.isDigit(sql.charAt(i))) i++;
                tokens.add(new Token("NUMBER", sql.substring(start, i)));
                continue;
            }

            // идентификатор или ключевое слово
            if (Character.isLetter(ch) || ch == '_') {
                int start = i;
                i++;
                while (i < sql.length() &&
                        (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    i++;
                }
                String word = sql.substring(start, i);
                String upper = word.toUpperCase();
                if (isKeyword(upper)) {
                    tokens.add(new Token(upper, word));
                } else {
                    tokens.add(new Token("IDENT", word));
                }
                continue;
            }

            throw new RuntimeException("Unexpected character in SQL: '" + ch + "' at position " + i);
        }

        return tokens;
    }

    private boolean isOperatorStart(char c) {
        return c == '>' || c == '<' || c == '=';
    }

    private boolean isKeyword(String upper) {
        return switch (upper) {
            case "SELECT", "FROM", "WHERE", "CREATE", "TABLE", "INSERT", "INTO",
                    "VALUES", "INDEX", "ON", "USING", "HASH", "BTREE",
                    "RESET", "DATABASE", "ALL" -> true;
            default -> false;
        };
    }

    public static void main(String[] args) {
        SimpleLexer simpleLexer = new SimpleLexer();
        String sql = "SELECT name, age FROM users WHERE age > 18;";
        var arr = simpleLexer.tokenize(sql);
        for (var i : arr) {
            System.out.println(i);
        }
        System.out.println(arr);
    }
}
