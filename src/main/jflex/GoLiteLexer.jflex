package com.golite.lexer;

import com.golite.reports.ErrorType;
import com.golite.reports.ReportCollector;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

%%

%public
%class GoLiteLexer
%unicode
%line
%column
%type Token
%function yylex

%state STRING
%state RUNE
%state BLOCK_COMMENT

%{
    // este colector guarda los errores lexicos que aparecen durante el recorrido.
    private final ReportCollector reportCollector;

    // esta lista conserva todos los tokens reconocidos por el scanner.
    private final List<Token> tokens = new ArrayList<>();

    // este buffer arma cadenas y runas antes de convertirlas en token.
    private final StringBuilder buffer = new StringBuilder();

    // estas posiciones guardan el inicio real del token actual.
    private int tokenLine;
    private int tokenColumn;

    // este valor ayuda a decidir si un salto de linea debe generar punto y coma.
    private TokenType previousTokenType;

    /** crea un lexer nuevo desde texto en memoria. */
    public GoLiteLexer(String source, ReportCollector reportCollector) {
        this(new StringReader(source == null ? "" : source), reportCollector);
    }

    /** crea un lexer nuevo desde cualquier reader. */
    public GoLiteLexer(Reader reader, ReportCollector reportCollector) {
        this.zzReader = reader;
        this.reportCollector = reportCollector;
    }

    /** recorre todo el archivo y devuelve la lista completa de tokens. */
    public List<Token> scanTokens() throws IOException {
        Token token;
        while ((token = yylex()) != null) {
            tokens.add(token);
            previousTokenType = token.type();
            if (token.type() == TokenType.EOF) {
                break;
            }
        }
        return List.copyOf(tokens);
    }

    /** crea un token usando la posicion actual del scanner. */
    private Token buildToken(TokenType type, Object literal) {
        return new Token(type, yytext(), literal, tokenLine, tokenColumn);
    }

    /** crea un token usando una imagen textual ya armada a mano. */
    private Token buildToken(TokenType type, Object literal, String lexeme, int line, int column) {
        return new Token(type, lexeme, literal, line, column);
    }

    /** registra un error lexico sin detener el escaneo. */
    private void lexicalError(String message) {
        reportCollector.addError(ErrorType.LEXICAL, yyline + 1, yycolumn + 1, message);
    }

    /** prepara el scanner para leer una cadena. */
    private void startString() {
        buffer.setLength(0);
        tokenLine = yyline + 1;
        tokenColumn = yycolumn + 1;
        yybegin(STRING);
    }

    /** prepara el scanner para leer una runa. */
    private void startRune() {
        buffer.setLength(0);
        tokenLine = yyline + 1;
        tokenColumn = yycolumn + 1;
        yybegin(RUNE);
    }

    /** cierra una cadena y la convierte en token. */
    private Token finishString() {
        yybegin(YYINITIAL);
        return buildToken(TokenType.STRING_LITERAL, buffer.toString(), "\"" + buffer + "\"", tokenLine, tokenColumn);
    }

    /** cierra una runa y valida que tenga un solo caracter. */
    private Token finishRune() {
        yybegin(YYINITIAL);
        if (buffer.codePointCount(0, buffer.length()) != 1) {
            lexicalError("runa invalida");
            return null;
        }
        int codePoint = buffer.codePointAt(0);
        return buildToken(TokenType.RUNE_LITERAL, codePoint, "'" + buffer + "'", tokenLine, tokenColumn);
    }

    /** decodifica escapes simples para cadenas y runas. */
    private char decodeEscape(char escaped) {
        return switch (escaped) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case '"' -> '"';
            case '\'' -> '\'';
            case '\\' -> '\\';
            default -> escaped;
        };
    }

    /** decide si un salto de linea puede convertirse en punto y coma. */
    private boolean canInsertSemicolon(TokenType type) {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case IDENTIFIER, INT_LITERAL, FLOAT_LITERAL, STRING_LITERAL, RUNE_LITERAL,
                 BOOL_LITERAL, NIL_LITERAL, RPAREN, RBRACE, BREAK, CONTINUE -> true;
            default -> false;
        };
    }
%}

%%

<YYINITIAL>[ \t\f\r]+ {
    return null;
}

<YYINITIAL>\ufeff {
    return null;
}

<YYINITIAL>\n {
    if (canInsertSemicolon(previousTokenType)) {
        tokenLine = yyline + 1;
        tokenColumn = yycolumn + 1;
        return buildToken(TokenType.SEMICOLON, null, ";", tokenLine, tokenColumn);
    }
    return null;
}

<YYINITIAL>//[^\n\r]* {
    return null;
}

<YYINITIAL>"/*" {
    yybegin(BLOCK_COMMENT);
    return null;
}

<BLOCK_COMMENT>"*/" {
    yybegin(YYINITIAL);
    return null;
}

<BLOCK_COMMENT><<EOF>> {
    lexicalError("comentario de bloque sin cerrar");
    return buildToken(TokenType.EOF, null, "", yyline + 1, yycolumn + 1);
}

<BLOCK_COMMENT>(.|\n) {
    return null;
}

<YYINITIAL>"(" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.LPAREN, null);
}

<YYINITIAL>")" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.RPAREN, null);
}

<YYINITIAL>"{" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.LBRACE, null);
}

<YYINITIAL>"}" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.RBRACE, null);
}

<YYINITIAL>"," {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.COMMA, null);
}

<YYINITIAL>";" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.SEMICOLON, null);
}

<YYINITIAL>":=" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.COLON_EQUAL, null);
}

<YYINITIAL>"." {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.DOT, null);
}

<YYINITIAL>"&&" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.AND_AND, null);
}

<YYINITIAL>"||" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.OR_OR, null);
}

<YYINITIAL>"==" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.EQUAL_EQUAL, null);
}

<YYINITIAL>"!=" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.BANG_EQUAL, null);
}

<YYINITIAL>"<=" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.LESS_EQUAL, null);
}

<YYINITIAL>">=" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.GREATER_EQUAL, null);
}

<YYINITIAL>"=" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.ASSIGN, null);
}

<YYINITIAL>"<" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.LESS, null);
}

<YYINITIAL>">" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.GREATER, null);
}

<YYINITIAL>"+" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.PLUS, null);
}

<YYINITIAL>"-" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.MINUS, null);
}

<YYINITIAL>"*" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.STAR, null);
}

<YYINITIAL>"/" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.SLASH, null);
}

<YYINITIAL>"%" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.PERCENT, null);
}

<YYINITIAL>"!" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.BANG, null);
}

<YYINITIAL>"\"" {
    startString();
}

<YYINITIAL>"'" {
    startRune();
}

<STRING>"\"" {
    return finishString();
}

<STRING>\\[ntr"'\\] {
    buffer.append(decodeEscape(yytext().charAt(1)));
}

<STRING>[^\\"\n]+ {
    buffer.append(yytext());
}

<STRING>\n {
    lexicalError("cadena sin cerrar");
    yybegin(YYINITIAL);
}

<STRING><<EOF>> {
    lexicalError("cadena sin cerrar");
    return buildToken(TokenType.EOF, null, "", yyline + 1, yycolumn + 1);
}

<RUNE>"'" {
    Token token = finishRune();
    if (token != null) {
        return token;
    }
}

<RUNE>\\[ntr"'\\] {
    buffer.append(decodeEscape(yytext().charAt(1)));
}

<RUNE>[^\\'\n]+ {
    buffer.append(yytext());
}

<RUNE>\n {
    lexicalError("runa sin cerrar");
    yybegin(YYINITIAL);
}

<RUNE><<EOF>> {
    lexicalError("runa sin cerrar");
    return buildToken(TokenType.EOF, null, "", yyline + 1, yycolumn + 1);
}

<YYINITIAL>"var" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.VAR, null);
}

<YYINITIAL>"if" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.IF, null);
}

<YYINITIAL>"else" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.ELSE, null);
}

<YYINITIAL>"for" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.FOR, null);
}

<YYINITIAL>"break" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.BREAK, null);
}

<YYINITIAL>"continue" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.CONTINUE, null);
}

<YYINITIAL>"true" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.BOOL_LITERAL, Boolean.TRUE);
}

<YYINITIAL>"false" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.BOOL_LITERAL, Boolean.FALSE);
}

<YYINITIAL>"nil" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.NIL_LITERAL, null);
}

<YYINITIAL>"int" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.TYPE_INT, null);
}

<YYINITIAL>"float64" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.TYPE_FLOAT64, null);
}

<YYINITIAL>"bool" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.TYPE_BOOL, null);
}

<YYINITIAL>"string" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.TYPE_STRING, null);
}

<YYINITIAL>"rune" {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.TYPE_RUNE, null);
}

<YYINITIAL>[0-9]+"."[0-9]+ {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.FLOAT_LITERAL, Double.valueOf(yytext()));
}

<YYINITIAL>[0-9]+ {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.INT_LITERAL, Long.valueOf(yytext()));
}

<YYINITIAL>[A-Za-z_][A-Za-z0-9_]* {
    tokenLine = yyline + 1;
    tokenColumn = yycolumn + 1;
    return buildToken(TokenType.IDENTIFIER, null);
}

<YYINITIAL>. {
    lexicalError("caracter no reconocido: '" + yytext() + "'");
}

<YYINITIAL><<EOF>> {
    return buildToken(TokenType.EOF, null, "", yyline + 1, yycolumn + 1);
}
