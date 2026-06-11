package com.golite.runtime;

import com.golite.ast.BlockStmt;
import com.golite.ast.CallExpr;
import com.golite.ast.Expr;
import com.golite.ast.ForStmt;
import com.golite.ast.IfStmt;
import com.golite.ast.Program;
import com.golite.ast.Stmt;
import com.golite.ast.VarDeclStmt;
import com.golite.lexer.TokenType;
import com.golite.reports.ErrorType;
import com.golite.reports.ReportCollector;

import java.util.List;

/** ejecuta el ast y aplica las reglas de fase 1 de forma manual. */
public final class Interpreter {

    private final ReportCollector reportCollector;
    private final Environment globals = new Environment();
    private final StringBuilder output = new StringBuilder();

    public Interpreter(ReportCollector reportCollector) {
        this.reportCollector = reportCollector;
    }

    public String execute(Program program) {
        executeBlock(program.statements(), globals);
        return output.toString();
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment local = environment == globals ? globals : new Environment(environment);
        for (Stmt statement : statements) {
            try {
                statement.execute(this, local);
            } catch (BreakSignal | ContinueSignal signal) {
                throw signal;
            } catch (RuntimeException exception) {
                if (reportCollector.hasErrors()) {
                    continue;
                }
                reportCollector.addError(ErrorType.SEMANTIC, 0, 0, exception.getMessage());
            }
        }
    }

    public void executeIf(IfStmt ifStmt, Environment environment) {
        if (truthy(ifStmt.condition().evaluate(this, environment))) {
            ifStmt.thenBranch().execute(this, environment);
            return;
        }
        if (ifStmt.elseBranch() != null) {
            ifStmt.elseBranch().execute(this, environment);
        }
    }

    public void executeFor(ForStmt forStmt, Environment environment) {
        Environment loopEnvironment = new Environment(environment);
        if (forStmt.initializer() != null) {
            forStmt.initializer().execute(this, loopEnvironment);
        }

        while (forStmt.condition() == null || truthy(forStmt.condition().evaluate(this, loopEnvironment))) {
            try {
                forStmt.body().execute(this, loopEnvironment);
            } catch (ContinueSignal signal) {
                if (forStmt.post() != null) {
                    forStmt.post().execute(this, loopEnvironment);
                }
                continue;
            } catch (BreakSignal signal) {
                break;
            }

            if (forStmt.post() != null) {
                forStmt.post().execute(this, loopEnvironment);
            }
        }
    }

    public void executeVarDecl(VarDeclStmt varDeclStmt, Environment environment) {
        Value value = varDeclStmt.initializer() == null
                ? defaultValue(varDeclStmt.declaredType())
                : varDeclStmt.initializer().evaluate(this, environment);

        ValueType declaredType = varDeclStmt.declaredType();
        if (declaredType == null && value != null) {
            declaredType = value.type();
        }
        if (declaredType != null) {
            value = coerceValue(value, declaredType);
        }

        environment.define(varDeclStmt.name(), declaredType, value == null ? Value.voidValue() : value);
    }

    public Value assignValue(String name, Value value, Environment environment) {
        ValueType declaredType = environment.getDeclaredType(name);
        if (declaredType != null) {
            value = coerceValue(value, declaredType);
        }
        environment.assign(name, value);
        return value;
    }

    public Value evaluateUnary(TokenType operator, Value right) {
        return switch (operator) {
            case BANG -> Value.bool(!truthy(right));
            case MINUS -> {
                requireNumeric(right, "el operador '-' requiere un valor numerico");
                if (right.type() == ValueType.FLOAT) {
                    yield Value.ofFloat(-right.asFloat());
                }
                yield Value.ofInt(-right.asInt());
            }
            default -> throw semanticException("operador unario no soportado");
        };
    }

    public Value evaluateBinary(TokenType operator, Value left, Value right) {
        return switch (operator) {
            case PLUS -> add(left, right);
            case MINUS -> numeric(left, right, operator, false);
            case STAR -> numeric(left, right, operator, false);
            case SLASH -> numeric(left, right, operator, true);
            case PERCENT -> modulo(left, right);
            case GREATER, GREATER_EQUAL, LESS, LESS_EQUAL -> compare(left, right, operator);
            case EQUAL_EQUAL -> Value.bool(equals(left, right));
            case BANG_EQUAL -> Value.bool(!equals(left, right));
            case AND_AND -> Value.bool(truthy(left) && truthy(right));
            case OR_OR -> Value.bool(truthy(left) || truthy(right));
            default -> throw semanticException("operador binario no soportado");
        };
    }

    public Value evaluateCall(CallExpr callExpr, Environment environment) {
        List<Value> arguments = callExpr.arguments().stream()
                .map(argument -> argument.evaluate(this, environment))
                .toList();

        return switch (callExpr.target()) {
            case "fmt.Println" -> {
                appendOutput(arguments, true);
                yield Value.voidValue();
            }
            case "fmt.Print" -> {
                appendOutput(arguments, false);
                yield Value.voidValue();
            }
            case "strconv.Atoi" -> atoi(arguments, callExpr.target());
            case "strconv.Itoa" -> itoa(arguments, callExpr.target());
            case "strconv.ParseFloat" -> parseFloat(arguments, callExpr.target());
            default -> throw semanticException("funcion embebida no soportada: " + callExpr.target());
        };
    }

    private Value add(Value left, Value right) {
        if (left.type() == ValueType.STRING || right.type() == ValueType.STRING) {
            if (left.type() != ValueType.STRING || right.type() != ValueType.STRING) {
                throw semanticException("la suma de texto solo acepta cadenas en ambos lados");
            }
            return Value.ofString(left.asString() + right.asString());
        }
        requireNumeric(left, "el operador '+' requiere valores numericos o cadenas");
        requireNumeric(right, "el operador '+' requiere valores numericos o cadenas");
        if (left.type() == ValueType.FLOAT || right.type() == ValueType.FLOAT) {
            return Value.ofFloat(left.asFloat() + right.asFloat());
        }
        if (left.type() == ValueType.RUNE || right.type() == ValueType.RUNE) {
            return Value.ofRune((int) (left.asInt() + right.asInt()));
        }
        return Value.ofInt(left.asInt() + right.asInt());
    }

    private Value numeric(Value left, Value right, TokenType operator, boolean division) {
        requireNumeric(left, "el operador numerico requiere valores enteros o flotantes");
        requireNumeric(right, "el operador numerico requiere valores enteros o flotantes");
        if (division && isZero(right)) {
            throw semanticException("division entre cero");
        }

        boolean useFloat = left.type() == ValueType.FLOAT || right.type() == ValueType.FLOAT || division;
        double leftValue = left.asFloat();
        double rightValue = right.asFloat();
        double result = switch (operator) {
            case MINUS -> leftValue - rightValue;
            case STAR -> leftValue * rightValue;
            case SLASH -> leftValue / rightValue;
            default -> throw semanticException("operador numerico no soportado");
        };

        if (useFloat) {
            return Value.ofFloat(result);
        }
        return Value.ofInt((long) result);
    }

    private Value modulo(Value left, Value right) {
        if (left.type() != ValueType.INT || right.type() != ValueType.INT) {
            throw semanticException("el operador '%' solo acepta enteros");
        }
        if (right.asInt() == 0) {
            throw semanticException("division entre cero");
        }
        return Value.ofInt(left.asInt() % right.asInt());
    }

    private Value compare(Value left, Value right, TokenType operator) {
        requireNumeric(left, "la comparacion requiere valores numericos");
        requireNumeric(right, "la comparacion requiere valores numericos");

        double leftValue = left.asFloat();
        double rightValue = right.asFloat();
        boolean result = switch (operator) {
            case GREATER -> leftValue > rightValue;
            case GREATER_EQUAL -> leftValue >= rightValue;
            case LESS -> leftValue < rightValue;
            case LESS_EQUAL -> leftValue <= rightValue;
            default -> throw semanticException("operador de comparacion no soportado");
        };
        return Value.bool(result);
    }

    private boolean equals(Value left, Value right) {
        if (left.type() == right.type()) {
            return switch (left.type()) {
                case INT -> left.asInt() == right.asInt();
                case FLOAT -> Double.compare(left.asFloat(), right.asFloat()) == 0;
                case STRING -> left.asString().equals(right.asString());
                case RUNE -> left.asInt() == right.asInt();
                case BOOL -> left.asBoolean() == right.asBoolean();
                case NIL -> true;
                case VOID -> true;
            };
        }
        if (left.isNumeric() && right.isNumeric()) {
            return Double.compare(left.asFloat(), right.asFloat()) == 0;
        }
        return false;
    }

    private boolean truthy(Value value) {
        if (value == null || value.type() == ValueType.VOID) {
            return false;
        }
        if (value.type() == ValueType.NIL) {
            return false;
        }
        if (value.type() == ValueType.BOOL) {
            return value.asBoolean();
        }
        if (value.isNumeric()) {
            return value.asFloat() != 0.0d;
        }
        if (value.type() == ValueType.STRING) {
            return !value.asString().isEmpty();
        }
        return false;
    }

    private Value defaultValue(ValueType type) {
        if (type == null) {
            return Value.voidValue();
        }
        return switch (type) {
            case INT -> Value.ofInt(0);
            case FLOAT -> Value.ofFloat(0.0d);
            case STRING -> Value.ofString("");
            case RUNE -> Value.ofRune(0);
            case BOOL -> Value.bool(false);
            case NIL -> Value.nil();
            case VOID -> Value.voidValue();
        };
    }

    private Value coerceValue(Value value, ValueType targetType) {
        if (value == null || targetType == null || value.type() == targetType) {
            return value == null ? Value.voidValue() : value;
        }
        return switch (targetType) {
            case INT -> Value.ofInt(value.asInt());
            case FLOAT -> Value.ofFloat(value.asFloat());
            case STRING -> Value.ofString(value.asString());
            case RUNE -> Value.ofRune((int) value.asInt());
            case BOOL -> Value.bool(truthy(value));
            case NIL -> Value.nil();
            case VOID -> Value.voidValue();
        };
    }

    private void requireNumeric(Value value, String message) {
        if (!value.isNumeric()) {
            throw semanticException(message);
        }
    }

    private boolean isZero(Value value) {
        return value.isNumeric() && value.asFloat() == 0.0d;
    }

    private void appendOutput(List<Value> arguments, boolean newline) {
        for (int index = 0; index < arguments.size(); index++) {
            if (index > 0) {
                output.append(' ');
            }
            output.append(arguments.get(index).asString());
        }
        if (newline) {
            output.append(System.lineSeparator());
        }
    }

    private Value atoi(List<Value> arguments, String target) {
        requireArgumentCount(arguments, 1, target);
        return Value.ofInt(parseLongText(arguments.get(0).asString().trim()));
    }

    private Value itoa(List<Value> arguments, String target) {
        requireArgumentCount(arguments, 1, target);
        Value argument = arguments.get(0);
        requireNumeric(argument, target + " requiere un valor numerico");
        return Value.ofString(Long.toString(argument.asInt()));
    }

    private Value parseFloat(List<Value> arguments, String target) {
        requireArgumentCount(arguments, 1, target);
        return Value.ofFloat(parseDoubleText(arguments.get(0).asString().trim()));
    }

    private void requireArgumentCount(List<Value> arguments, int expected, String target) {
        if (arguments.size() != expected) {
            throw semanticException(target + " espera " + expected + " argumento(s)");
        }
    }

    private long parseLongText(String text) {
        if (text.isEmpty()) {
            throw semanticException("no se puede convertir una cadena vacia a entero");
        }

        int index = 0;
        boolean negative = false;
        if (text.charAt(0) == '+' || text.charAt(0) == '-') {
            negative = text.charAt(0) == '-';
            index++;
        }

        long value = 0;
        for (; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c < '0' || c > '9') {
                throw semanticException("cadena invalida para entero: " + text);
            }
            value = value * 10 + (c - '0');
        }
        return negative ? -value : value;
    }

    private double parseDoubleText(String text) {
        if (text.isEmpty()) {
            throw semanticException("no se puede convertir una cadena vacia a flotante");
        }

        int index = 0;
        boolean negative = false;
        if (text.charAt(0) == '+' || text.charAt(0) == '-') {
            negative = text.charAt(0) == '-';
            index++;
        }

        double value = 0.0d;
        while (index < text.length() && text.charAt(index) != '.') {
            char c = text.charAt(index);
            if (c < '0' || c > '9') {
                throw semanticException("cadena invalida para flotante: " + text);
            }
            value = value * 10.0d + (c - '0');
            index++;
        }

        if (index < text.length() && text.charAt(index) == '.') {
            index++;
            double divisor = 10.0d;
            while (index < text.length()) {
                char c = text.charAt(index);
                if (c < '0' || c > '9') {
                    throw semanticException("cadena invalida para flotante: " + text);
                }
                value += (c - '0') / divisor;
                divisor *= 10.0d;
                index++;
            }
        }

        return negative ? -value : value;
    }

    private RuntimeException semanticException(String message) {
        return new IllegalStateException(message);
    }

    public static final class BreakSignal extends RuntimeException {
    }

    public static final class ContinueSignal extends RuntimeException {
    }
}