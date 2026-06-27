package com.golite.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class GoLiteStruct {
    public final String typeName; // ej. "Producto"
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public GoLiteStruct(String typeName) {
        this.typeName = typeName;
    }

    public void setField(String name, Object value) {
        fields.put(name, value);
    }

    public Object getField(String name) {
        if (!fields.containsKey(name)) {
            throw new RuntimeException("Error Semántico: el struct '" + typeName + "' no tiene el campo '" + name + "'.");
        }
        return fields.get(name);
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public Map<String, Object> getAllFields() {
        return fields;
    }

    // Representacion estilo Go: NombreStruct{campo1:valor1 campo2:valor2}
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(typeName).append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) sb.append(" ");
            first = false;
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}