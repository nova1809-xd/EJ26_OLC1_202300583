package com.golite.runtime;

// no es un error real: se usa como señal interna para que el ForStmt
// que la atrapa sepa que debe saltar a la siguiente iteracion.
public class ContinueException extends RuntimeException {
    public ContinueException() {
        super(null, null, false, false); // sin stacktrace, mas rapido
    }
}
