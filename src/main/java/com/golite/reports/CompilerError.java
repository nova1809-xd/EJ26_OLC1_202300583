package com.golite.reports;

/** representa un error detectado durante el analisis o la ejecucion. */
public record CompilerError(ErrorType type, String message, int line, int column) {
}