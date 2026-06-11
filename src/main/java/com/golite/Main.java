package com.golite;

import com.golite.app.GoLiteApplication;

import java.nio.file.Path;

/** punto de entrada de la aplicacion */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("uso: mvn exec:java -Dexec.args=\"ruta/al/archivo.golite\"");
            return;
        }

        GoLiteApplication application = new GoLiteApplication();
        GoLiteApplication.RunResult result = application.run(Path.of(args[0]));

        System.out.println("== tabla de tokens ==");
        result.tokens().forEach(token -> System.out.println(token.line() + ":" + token.column() + " " + token.type() + " " + token.lexeme()));

        System.out.println();
        System.out.println("== salida ==");
        if (result.output().isBlank()) {
            System.out.println("<sin salida>");
        } else {
            System.out.print(result.output());
        }

        System.out.println();
        System.out.println("== errores ==");
        if (result.errors().isEmpty()) {
            System.out.println("<sin errores>");
            return;
        }

        result.errors().forEach(error -> System.out.println(error.type() + " " + error.line() + ":" + error.column() + " " + error.message()));
    }
}