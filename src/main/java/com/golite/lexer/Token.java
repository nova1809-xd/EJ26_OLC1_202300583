package com.golite.lexer;

public class Token {
    private int line;
    private int column;
    private String lexeme;
    private String type;

    public Token(int line, int column, String lexeme, String type) {
        this.line = line;
        this.column = column;
        this.lexeme = lexeme;
        this.type = type;
    }

    public int line() { return line; }
    public int column() { return column; }
    public String lexeme() { return lexeme; }
    public String type() { return type; }
}