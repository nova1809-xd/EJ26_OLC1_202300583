package com.golite.runtime;

// No es un error real: se usa como señal interna para que el ForStmt
// que la atrapa sepa que debe detener el bucle.
public class BreakException extends RuntimeException {
    public BreakException() {
        super(null, null, false, false); // sin stacktrace, mas rapido
    }
}