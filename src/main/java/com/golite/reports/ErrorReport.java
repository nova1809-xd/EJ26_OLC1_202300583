package com.golite.reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** centraliza los errores lexicos, sintacticos y semanticos. */
public final class ErrorReport {

    private final List<CompilerError> errors = new ArrayList<>();

    public void addError(ErrorType type, int line, int column, String message) {
        errors.add(new CompilerError(type, message, line, column));
    }

    public List<CompilerError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}