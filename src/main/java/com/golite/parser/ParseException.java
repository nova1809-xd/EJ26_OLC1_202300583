package com.golite.parser;

/** indica que el parser encontro una estructura invalida. */
public final class ParseException extends RuntimeException {

    public ParseException(String message) {
        super(message);
    }
}