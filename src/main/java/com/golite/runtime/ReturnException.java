package com.golite.runtime;

// no es un error real: se usa para "saltar" toda la ejecucion del cuerpo de
// una funcion de vuelta al punto donde se hizo la llamada, cargando el valor
// que se debe retornar (o null si fue un "return" sin valor).
public class ReturnException extends RuntimeException {
    public final Object value;

    public ReturnException(Object value) {
        super(null, null, false, false); // sin stacktrace, mas rapido
        this.value = value;
    }
}
