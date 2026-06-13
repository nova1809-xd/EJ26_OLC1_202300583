package com.golite;

import com.golite.app.GoLiteIDE;
import javax.swing.SwingUtilities;

/** punto de entrada de la aplicacion */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // levanto la interfaz grafica
        SwingUtilities.invokeLater(() -> {
            GoLiteIDE ide = new GoLiteIDE();
            ide.setVisible(true);
            System.out.println("lexer nuevo carg");
        });
    }
    
}