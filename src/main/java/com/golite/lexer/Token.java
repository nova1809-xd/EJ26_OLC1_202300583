package com.golite.lexer;

/** representa un token identificado por el analizador lexico. */
public record Token(TokenType type, String lexeme, Object literal, int line, int column) {
}