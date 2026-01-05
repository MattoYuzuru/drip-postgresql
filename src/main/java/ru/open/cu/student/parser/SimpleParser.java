package ru.open.cu.student.parser;

import ru.open.cu.student.lexer.Lexer;
import ru.open.cu.student.lexer.SimpleLexer;
import ru.open.cu.student.lexer.Token;
import ru.open.cu.student.parser.nodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class SimpleParser implements Parser {

    @Override
    public AstNode parse(List<Token> tokens) {
        Cursor c = new Cursor(tokens);
        AstNode stmt;
        if (c.peekIsType("CREATE")) {
            stmt = parseCreate(c);
        } else if (c.peekIsType("INSERT")) {
            stmt = parseInsert(c);
        } else if (c.peekIsType("RESET")) {
            stmt = parseReset(c);
        } else {
            if (!c.peekIsType("SELECT")) {
                throw c.error("Неизвестная команда. Смотрите howto.md");
            }
            stmt = parseSelect(c);
        }

        c.matchTypeOpt("SEMICOLON");
        if (!c.eof()) {
            throw c.error("Лишние токены с: " + c.describe(tokens.get(c.pos)));
        }
        return stmt;
    }

    private AstNode parseCreate(Cursor c) {
        c.expectType("CREATE");
        if (c.matchTypeOpt("TABLE")) {
            return parseCreateTable(c);
        } else if (c.matchTypeOpt("INDEX")) {
            return parseCreateIndex(c);
        }
        throw c.error("Ожидался TABLE или INDEX после CREATE");
    }

    private AstNode parseCreateTable(Cursor c) {
        String tableName = c.expectIdent();
        c.expectType("LPAREN");
        List<ColumnDef> columns = new ArrayList<>();
        columns.add(parseColumnDef(c));
        while (c.matchTypeOpt("COMMA")) {
            columns.add(parseColumnDef(c));
        }
        c.expectType("RPAREN");
        return new CreateTableStmt(tableName, columns);
    }

    private ColumnDef parseColumnDef(Cursor c) {
        String name = c.expectIdent();
        String type = c.expectIdentOrKeyword();
        if (c.matchTypeOpt("LPAREN")) {
            String len = c.expectNumberLiteral();
            c.expectType("RPAREN");
            type = type + "(" + len + ")";
        }
        return new ColumnDef(name, type);
    }

    private AstNode parseCreateIndex(Cursor c) {
        String indexName = c.expectIdent();
        c.expectType("ON");
        String tableName = c.expectIdent();
        c.expectType("LPAREN");
        String columnName = c.expectIdent();
        c.expectType("RPAREN");
        String indexType = null;
        if (c.matchTypeOpt("USING")) {
            indexType = c.expectIdentOrKeyword();
        }
        return new CreateIndexStmt(indexName, tableName, columnName, indexType);
    }

    private AstNode parseInsert(Cursor c) {
        c.expectType("INSERT");
        c.expectType("INTO");
        String tableName = c.expectIdent();
        c.expectType("VALUES");
        c.expectType("LPAREN");
        List<Expr> values = new ArrayList<>();
        values.add(parsePrimary(c));
        while (c.matchTypeOpt("COMMA")) {
            values.add(parsePrimary(c));
        }
        c.expectType("RPAREN");
        return new InsertStmt(tableName, values);
    }

    private AstNode parseReset(Cursor c) {
        c.expectType("RESET");
        String scope = null;
        if (!c.eof() && (c.peekIsType("IDENT") || c.peekIsType("DATABASE") || c.peekIsType("ALL"))) {
            scope = c.next().value;
        }
        return new ResetStmt(scope);
    }

    private SelectStmt parseSelect(Cursor c) {
        c.expectType("SELECT");
        List<ResTarget> targets = parseTargetList(c);

        c.expectType("FROM");
        List<RangeVar> from = parseFromList(c);

        Expr where = null;
        if (c.matchTypeOpt("WHERE")) {
            where = parseExpr(c);
        }
        return new SelectStmt(targets, from, where);
    }

    private List<ResTarget> parseTargetList(Cursor c) {
        List<ResTarget> list = new ArrayList<>();
        if (c.peekIsType("STAR")) {
            c.next();
            list.add(new ResTarget(new ColumnRef("*"), null));
            return list;
        }
        list.add(parseTarget(c));
        while (c.matchTypeOpt("COMMA")) {
            list.add(parseTarget(c));
        }
        return list;
    }

    private ResTarget parseTarget(Cursor c) {
        ColumnRef col = parseColumnRef(c);
        return new ResTarget(col, null);
    }

    private List<RangeVar> parseFromList(Cursor c) {
        List<RangeVar> list = new ArrayList<>();
        list.add(parseRangeVar(c));
        return list;
    }

    private RangeVar parseRangeVar(Cursor c) {
        String ident = c.expectIdent();
        String alias = null;
        if (c.peekIsType("IDENT")) {
            alias = c.expectIdent();
        }
        return new RangeVar(ident, alias);
    }

    private ColumnRef parseColumnRef(Cursor c) {
        String ident = c.expectIdent();
        if (c.matchTypeOpt("DOT")) {
            if (c.peekIsType("STAR")) {
                c.next();
                ident = ident + ".*";
            } else {
                String tail = c.expectIdent();
                ident = ident + "." + tail;
            }
        }
        return new ColumnRef(ident);
    }

    private Expr parseExpr(Cursor c) {
        Expr left = parsePrimary(c);

        if (c.peekIsType("OP")) {
            Token opTok = c.next();
            String op = opTok.value;
            Expr right = parsePrimary(c);
            return new AExpr(op, left, right);
        }
        return left;
    }

    private Expr parsePrimary(Cursor c) {
        if (c.peekIsType("IDENT")) {
            return parseColumnRef(c);
        } else if (c.peekIsType("NUMBER")) {
            return new AConst(c.expectNumberLiteral());
        } else if (c.peekIsType("STRING")) {
            return new AConst(c.expectStringLiteral());
        } else {
            throw c.error("Ожидался идентификатор или число в выражении");
        }
    }

    private static final class Cursor {
        private final List<Token> tokens;
        private int pos = 0;

        Cursor(List<Token> tokens) {
            this.tokens = tokens == null ? List.of() : tokens;
        }

        boolean eof() {
            return pos >= tokens.size();
        }

        Token peek() {
            if (eof()) throw new NoSuchElementException("Конец потока токенов");
            return tokens.get(pos);
        }

        Token next() {
            Token t = peek();
            pos++;
            return t;
        }

        void expectType(String type) {
            Token t = next();
            if (!type.equalsIgnoreCase(t.type)) {
                throw error("Ожидался токен " + type + ", получен: " + describe(t));
            }
        }

        boolean matchTypeOpt(String type) {
            if (!eof() && type.equalsIgnoreCase(tokens.get(pos).type)) {
                pos++;
                return true;
            }
            return false;
        }

        boolean peekIsType(String type) {
            return !eof() && type.equalsIgnoreCase(tokens.get(pos).type);
        }

        String expectIdent() {
            Token t = next();
            if (!"IDENT".equalsIgnoreCase(t.type)) {
                throw error("Ожидался идентификатор, получен: " + describe(t));
            }
            return t.value;
        }

        String expectIdentOrKeyword() {
            Token t = next();
            if ("IDENT".equalsIgnoreCase(t.type) || (t.type != null && t.type.matches("[A-Z]+"))) {
                return t.value != null ? t.value : t.type;
            }
            throw error("Ожидался идентификатор или ключевое слово, получен: " + describe(t));
        }

        String expectNumberLiteral() {
            Token t = next();
            if (!"NUMBER".equalsIgnoreCase(t.type)) {
                throw error("Ожидалось число, получен: " + describe(t));
            }
            return t.value;
        }

        String expectStringLiteral() {
            Token t = next();
            if (!"STRING".equalsIgnoreCase(t.type)) {
                throw error("Ожидалась строка, получен: " + describe(t));
            }
            return t.value;
        }

        RuntimeException error(String msg) {
            return new RuntimeException("[Parser] " + msg + " (pos=" + pos + ")");
        }

        private String describe(Token t) {
            return "Token{value=" + t.value + ", type=" + t.type + "}";
        }
    }

    // test
    public static void main(String[] args) {
        Lexer lexer = new SimpleLexer();
        List<Token> tokens = lexer.tokenize("SELECT name, age FROM users WHERE age > 18;");

        Parser parser = new SimpleParser();
        AstNode ast = parser.parse(tokens);

        System.out.println(PrettyPrinter.print(ast));
    }
}
