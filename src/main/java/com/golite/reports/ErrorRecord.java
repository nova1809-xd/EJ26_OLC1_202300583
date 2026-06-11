package com.golite.reports;

/** guarda un error con su tipo, mensaje y posicion. */
public record ErrorRecord(ErrorType type, int line, int column, String message) {
}