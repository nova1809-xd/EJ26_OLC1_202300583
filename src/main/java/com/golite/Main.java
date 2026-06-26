package com.golite;

import com.golite.app.GoLiteIDE;

public class Main {
    public static void main(String[] args) {
        System.out.println("iniciando la interfaz grafica de mi compilador...");
        
        try {
            // creo una nueva instancia de mi ventana principal y la hago visible
            new GoLiteIDE().setVisible(true);
        } catch (Exception e) {
            System.err.println("error al iniciar la gui: " + e.getMessage());
        }
    }
}