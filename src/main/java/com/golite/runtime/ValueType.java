package com.golite.runtime;

import com.golite.lexer.TokenType;

/** tipado primitivo que maneja el interprete de fase 1. */
public enum ValueType {
    INT,
    FLOAT,
    STRING,
    RUNE,
    BOOL,
    NIL,
    VOID;

    public static ValueType fromKeyword(TokenType tokenType) {
        return switch (tokenType) {
            case TYPE_INT -> INT;
            case TYPE_FLOAT64 -> FLOAT;
            case TYPE_STRING -> STRING;
            case TYPE_BOOL -> BOOL;
            case TYPE_RUNE -> RUNE;
            default -> null;
        };
    }
}