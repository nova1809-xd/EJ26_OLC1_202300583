package com.golite.runtime;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    // Define una variable nueva (o la reemplaza si ya existia en este mismo ambito)
    public void define(String name, Object value) {
        values.put(name, value);
    }

    // Busca el valor de una variable. Si no existe, error semantico.
    public Object get(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        throw new RuntimeException("Error Semántico: variable no declarada '" + name + "'.");
    }

    // Asigna un nuevo valor a una variable que ya existe (para futuro: x = 5, x += 1)
    public void assign(String name, Object value) {
        if (values.containsKey(name)) {
            values.put(name, value);
            return;
        }
        throw new RuntimeException("Error Semántico: no se puede asignar a variable no declarada '" + name + "'.");
    }

    public boolean isDefined(String name) {
        return values.containsKey(name);
    }
}