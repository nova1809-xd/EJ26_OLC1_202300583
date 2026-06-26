package com.golite.runtime;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    // guardo mis variables de este bloque en este mapa
    private final Map<String, Object> values = new HashMap<>();
    
    // guardo la referencia a mi entorno padre (el bloque exterior)
    private final Environment enclosing;

    // constructor para mi entorno global (el mas grande, no tiene padre)
    public Environment() {
        this.enclosing = null;
    }

    // constructor para mis entornos locales (bloques que si tienen padre)
    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // declaro una nueva variable SIEMPRE en mi ambito actual (local o global)
    public void define(String name, Object value) {
        values.put(name, value);
    }

    // busco una variable, si no esta en mi bloque, le pregunto a mi padre
    public Object get(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeException("Error Semántico: Variable no definida '" + name + "'.");
    }

    // reasigno una variable, buscando en mi bloque o subiendo hasta encontrarla
    public void assign(String name, Object value) {
        if (values.containsKey(name)) {
            values.put(name, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeException("Error Semántico: Variable no definida '" + name + "'.");
    }
}