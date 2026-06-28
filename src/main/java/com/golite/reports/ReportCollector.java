package com.golite.reports;

import com.golite.lexer.Token;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** acumula tokens y errores para los reportes finales. */
public final class ReportCollector {
    
    // 1. Ahora son estáticos: una única lista universal para todo el programa
    private static final List<Token> tokens = new ArrayList<>();
    private static final List<ErrorRecord> errors = new ArrayList<>();

    // 2. Los métodos también son estáticos
    public static void addToken(Token token) {
        tokens.add(token);
    }

    public static void addError(ErrorType type, int line, int column, String message) {
        errors.add(new ErrorRecord(type, line, column, message));
    }

    public static List<Token> tokens() {
        return Collections.unmodifiableList(tokens);
    }

    public static List<ErrorRecord> errors() {
        return Collections.unmodifiableList(errors);
    }

    // 3. Método vital: Llámalo al darle clic al botón "Ejecutar" antes de compilar
    public static void clear() {
        tokens.clear();
        errors.clear();
    }
}