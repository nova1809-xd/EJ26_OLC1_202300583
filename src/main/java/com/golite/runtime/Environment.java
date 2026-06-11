package com.golite.runtime;

import java.util.HashMap;
import java.util.Map;

/** administra los alcances y las variables declaradas en cada bloque. */
public final class Environment {

    private final Environment enclosing;
    private final Map<String, Binding> values = new HashMap<>();

    public Environment() {
        this.enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(String name, ValueType type, Value value) {
        values.put(name, new Binding(type, value));
    }

    public Value get(String name) {
        Binding binding = values.get(name);
        if (binding != null) {
            return binding.value();
        }
        if (enclosing != null) {
            return enclosing.get(name);
        }
        throw new IllegalStateException("variable no declarada: " + name);
    }

    public ValueType getDeclaredType(String name) {
        Binding binding = values.get(name);
        if (binding != null) {
            return binding.type();
        }
        if (enclosing != null) {
            return enclosing.getDeclaredType(name);
        }
        return null;
    }

    public void assign(String name, Value value) {
        Binding binding = values.get(name);
        if (binding != null) {
            values.put(name, new Binding(binding.type(), value));
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new IllegalStateException("variable no declarada: " + name);
    }

    private record Binding(ValueType type, Value value) {
    }
}