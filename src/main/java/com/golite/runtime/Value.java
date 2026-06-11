package com.golite.runtime;

/** encapsula un valor runtime con su tipo primitivo. */
public final class Value {

    private final ValueType type;
    private final Object raw;

    private Value(ValueType type, Object raw) {
        this.type = type;
        this.raw = raw;
    }

    public static Value ofInt(long value) {
        return new Value(ValueType.INT, value);
    }

    public static Value ofFloat(double value) {
        return new Value(ValueType.FLOAT, value);
    }

    public static Value ofString(String value) {
        return new Value(ValueType.STRING, value);
    }

    public static Value ofRune(int value) {
        return new Value(ValueType.RUNE, value);
    }

    public static Value bool(boolean value) {
        return new Value(ValueType.BOOL, value);
    }

    public static Value nil() {
        return new Value(ValueType.NIL, null);
    }

    public static Value voidValue() {
        return new Value(ValueType.VOID, null);
    }

    public ValueType type() {
        return type;
    }

    public boolean isNumeric() {
        return type == ValueType.INT || type == ValueType.FLOAT || type == ValueType.RUNE;
    }

    public long asInt() {
        if (type == ValueType.INT) {
            return (Long) raw;
        }
        if (type == ValueType.FLOAT) {
            return (long) ((Double) raw).doubleValue();
        }
        if (type == ValueType.RUNE) {
            return (Integer) raw;
        }
        throw new IllegalStateException("no se puede convertir a entero");
    }

    public double asFloat() {
        if (type == ValueType.FLOAT) {
            return (Double) raw;
        }
        if (type == ValueType.INT) {
            return ((Long) raw).doubleValue();
        }
        if (type == ValueType.RUNE) {
            return ((Integer) raw).doubleValue();
        }
        throw new IllegalStateException("no se puede convertir a flotante");
    }

    public boolean asBoolean() {
        if (type == ValueType.BOOL) {
            return (Boolean) raw;
        }
        throw new IllegalStateException("no se puede convertir a booleano");
    }

    public boolean isNil() {
        return type == ValueType.NIL;
    }

    public String asString() {
        if (type == ValueType.STRING) {
            return (String) raw;
        }
        if (type == ValueType.INT) {
            return Long.toString((Long) raw);
        }
        if (type == ValueType.FLOAT) {
            return Double.toString((Double) raw);
        }
        if (type == ValueType.RUNE) {
            return Character.toString((char) ((Integer) raw).intValue());
        }
        if (type == ValueType.BOOL) {
            return Boolean.toString((Boolean) raw);
        }
        if (type == ValueType.NIL) {
            return "nil";
        }
        return "";
    }

    public Object raw() {
        return raw;
    }
}