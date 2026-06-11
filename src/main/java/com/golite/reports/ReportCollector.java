package com.golite.reports;

import com.golite.lexer.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** acumula tokens y errores para los reportes finales. */
public final class ReportCollector {

    private final List<Token> tokens = new ArrayList<>();
    private final List<ErrorRecord> errors = new ArrayList<>();

    public void addToken(Token token) {
        tokens.add(token);
    }

    public void addError(ErrorType type, int line, int column, String message) {
        errors.add(new ErrorRecord(type, line, column, message));
    }

    public List<Token> tokens() {
        return Collections.unmodifiableList(tokens);
    }

    public List<ErrorRecord> errors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}