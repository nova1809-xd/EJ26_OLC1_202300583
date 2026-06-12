package com.golite.lexer;

import com.golite.reports.ErrorType;
import com.golite.reports.ReportCollector;

import java.util.ArrayList;
import java.util.List;

/** lexer manual para convertir el texto fuente en una secuencia de tokens. */
public final class Lexer {

    private final String source;
    private final ReportCollector reportCollector;
    private final List<Token> tokens = new ArrayList<>();
    private int start;
    private int current;
    private int line = 1;
    private int column = 1;
    private int tokenLine = 1;
    private int tokenColumn = 1;
    private TokenType previousType;

    public Lexer(String source, ReportCollector reportCollector) {
        this.source = source == null ? "" : source;
        this.reportCollector = reportCollector;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            tokenLine = line;
            tokenColumn = column;
            scanToken();
        }
        addToken(TokenType.EOF, null);
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LPAREN, null); break;
            case ')': addToken(TokenType.RPAREN, null); break;
            case '{': addToken(TokenType.LBRACE, null); break;
            case '}': addToken(TokenType.RBRACE, null); break;
            case ',': addToken(TokenType.COMMA, null); break;
            case '.': addToken(TokenType.DOT, null); break;
            case '-': addToken(TokenType.MINUS, null); break;
            case '+': addToken(TokenType.PLUS, null); break;
            case '*': addToken(TokenType.STAR, null); break;
            case '%': addToken(TokenType.PERCENT, null); break;
            case ';': addToken(TokenType.SEMICOLON, null); break;
            case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG, null); break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.ASSIGN, null); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS, null); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER, null); break;
            case '&':
                if (match('&')) {
                    addToken(TokenType.AND_AND, null);
                } else {
                    lexicalError("se esperaba otro '&' para formar '&&'");
                }
                break;
            case '|':
                if (match('|')) {
                    addToken(TokenType.OR_OR, null);
                } else {
                    lexicalError("se esperaba otro '|' para formar '||'");
                }
                break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) {
                    consumeBlockComment();
                } else {
                    addToken(TokenType.SLASH, null);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\ufeff':
                break;
            case '\n':
                if (canInsertSemicolon(previousType)) {
                    addToken(TokenType.SEMICOLON, null);
                }
                line++;
                column = 1;
                break;
            case '"':
                readString();
                break;
            case '\'':
                readRune();
                break;
            default:
                if (isDigit(c)) {
                    readNumber();
                } else if (isAlpha(c)) {
                    readIdentifier();
                } else {
                    lexicalError("caracter no reconocido: '" + c + "'");
                }
                break;
        }
    }

    private void readString() {
        StringBuilder builder = new StringBuilder();
        boolean closed = false;

        while (!isAtEnd()) {
            char c = advance();
            if (c == '"') {
                closed = true;
                break;
            }
            if (c == '\\') {
                if (isAtEnd()) {
                    break;
                }
                char escaped = advance();
                switch (escaped) {
                    case 'n': builder.append('\n'); break;
                    case 't': builder.append('\t'); break;
                    case 'r': builder.append('\r'); break;
                    case '"': builder.append('"'); break;
                    case '\\': builder.append('\\'); break;
                    default: builder.append(escaped); break;
                }
                continue;
            }
            if (c == '\n') {
                line++;
                column = 1;
            }
            builder.append(c);
        }

        if (!closed) {
            lexicalError("cadena sin cerrar");
            return;
        }

        addToken(TokenType.STRING_LITERAL, builder.toString());
    }

    private void readNumber() {
        while (isDigit(peek())) {
            advance();
        }

        boolean isFloat = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isFloat = true;
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }

        String text = source.substring(start, current);
        if (isFloat) {
            addToken(TokenType.FLOAT_LITERAL, Double.parseDouble(text));
        } else {
            addToken(TokenType.INT_LITERAL, Long.parseLong(text));
        }
    }

    private void readIdentifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        String text = source.substring(start, current);
        TokenType type = switch (text) {
            case "var" -> TokenType.VAR;
            case "if" -> TokenType.IF;
            case "else" -> TokenType.ELSE;
            case "for" -> TokenType.FOR;
            case "break" -> TokenType.BREAK;
            case "continue" -> TokenType.CONTINUE;
            case "true", "false" -> TokenType.BOOL_LITERAL;
            case "nil" -> TokenType.NIL_LITERAL;
            case "int" -> TokenType.TYPE_INT;
            case "float64" -> TokenType.TYPE_FLOAT64;
            case "bool" -> TokenType.TYPE_BOOL;
            case "string" -> TokenType.TYPE_STRING;
            case "rune" -> TokenType.TYPE_RUNE;
            default -> TokenType.IDENTIFIER;
        };

        Object literal = null;
        if (type == TokenType.BOOL_LITERAL) {
            literal = Boolean.parseBoolean(text);
        }
        addToken(type, literal);
    }

    private void readRune() {
        if (isAtEnd()) {
            lexicalError("rune sin cerrar");
            return;
        }

        int value;
        char currentChar = advance();
        if (currentChar == '\\') {
            if (isAtEnd()) {
                lexicalError("rune sin cerrar");
                return;
            }
            char escaped = advance();
            value = switch (escaped) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case '\'' -> '\'';
                case '\\' -> '\\';
                default -> escaped;
            };
        } else {
            value = currentChar;
        }

        if (isAtEnd() || advance() != '\'') {
            lexicalError("rune sin cerrar");
            return;
        }

        addToken(TokenType.RUNE_LITERAL, value);
    }

    private void consumeBlockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }
            char c = advance();
            if (c == '\n') {
                line++;
                column = 1;
            }
        }
        lexicalError("comentario de bloque sin cerrar");
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }
        current++;
        column++;
        return true;
    }

    private char advance() {
        char c = source.charAt(current++);
        column++;
        return c;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        Token token = new Token(type, lexeme, literal, tokenLine, tokenColumn);
        tokens.add(token);
        reportCollector.addToken(token);
        previousType = type;
    }

    private void lexicalError(String message) {
        reportCollector.addError(ErrorType.LEXICAL, tokenLine, tokenColumn, message);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean canInsertSemicolon(TokenType type) {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case IDENTIFIER, INT_LITERAL, FLOAT_LITERAL, STRING_LITERAL, BOOL_LITERAL,
                 RUNE_LITERAL, NIL_LITERAL, RPAREN, RBRACE, BREAK, CONTINUE -> true;
            default -> false;
        };
    }
}