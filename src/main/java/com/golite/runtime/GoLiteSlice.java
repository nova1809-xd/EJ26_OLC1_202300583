package com.golite.runtime;

import java.util.ArrayList;
import java.util.List;

public class GoLiteSlice {
    public final String elementType; // ej. "int", "string", o "[]int" para [][]int
    private final List<Object> elements;

    public GoLiteSlice(String elementType, List<Object> elements) {
        this.elementType = elementType;
        this.elements = elements;
    }

    public static GoLiteSlice empty(String elementType) {
        return new GoLiteSlice(elementType, new ArrayList<>());
    }

    public int size() {
        return elements.size();
    }

    public Object get(int index) {
        if (index < 0 || index >= elements.size()) {
            throw new RuntimeException(
                "Error Semántico: índice " + index + " fuera de rango (tamaño del slice: " + elements.size() + ")."
            );
        }
        return elements.get(index);
    }

    public void set(int index, Object value) {
        if (index < 0 || index >= elements.size()) {
            throw new RuntimeException(
                "Error Semántico: índice " + index + " fuera de rango (tamaño del slice: " + elements.size() + ")."
            );
        }
        elements.set(index, value);
    }

    // append en Go retorna un slice NUEVO (no muta el original cuando hay
    // realojo de memoria). Para mantener la semantica simple y predecible en
    // esta fase, siempre devuelvo un nuevo GoLiteSlice con todos los elementos
    // anteriores mas el nuevo, dejando que el usuario reasigne el resultado
    // (igual que en Go real: numeros = append(numeros, 4)).
    public GoLiteSlice appended(Object value) {
        List<Object> newElements = new ArrayList<>(elements);
        newElements.add(value);
        return new GoLiteSlice(elementType, newElements);
    }

    public List<Object> getElements() {
        return elements;
    }

    // Representacion estilo Go: [1 2 3]
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(elements.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}