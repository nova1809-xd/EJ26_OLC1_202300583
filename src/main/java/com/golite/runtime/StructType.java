package com.golite.runtime;

import com.golite.ast.Param;
import java.util.List;

public class StructType {
    public final String name;
    public final List<Param> fields;

    public StructType(String name, List<Param> fields) {
        this.name = name;
        this.fields = fields;
    }

    public boolean hasField(String fieldName) {
        return fields.stream().anyMatch(f -> f.name.equals(fieldName));
    }

    public String fieldType(String fieldName) {
        for (Param f : fields) {
            if (f.name.equals(fieldName)) return f.type;
        }
        throw new RuntimeException("Error Semántico: el struct '" + name + "' no tiene el campo '" + fieldName + "'.");
    }
}