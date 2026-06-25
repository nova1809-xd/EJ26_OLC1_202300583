package com.golite.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;

public class GoLiteIDE extends JFrame {
    private JTextArea codeEditor;
    private JTextArea consoleOutput;
    // guardamos el resultado aqui para que los reportes lo puedan leer
    private GoLiteApplication.RunResult ultimoResultado = null;

    public GoLiteIDE() {
        // configuracion de la ventana principal
        setTitle("GoLite IDE");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // panel de arriba con el menu real y el boton
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topPanel.setBackground(new Color(230, 230, 230));
        
        JMenuBar menuBar = new JMenuBar();
        JMenu menuReportes = new JMenu("Reportes");
        JMenuItem repTokens = new JMenuItem("Reporte de Tokens");
        JMenuItem repErrores = new JMenuItem("Reporte de Errores");
        
        menuReportes.add(repTokens);
        menuReportes.add(repErrores);
        
        JMenu menuArchivo = new JMenu("Archivo");
        JMenuItem itemAbrir = new JMenuItem("Abrir Archivo...");
        menuArchivo.add(itemAbrir);
        menuBar.add(menuArchivo);
        menuBar.add(new JMenu("Herramientas"));
        menuBar.add(menuReportes);
        menuBar.add(new JMenu("Ayuda"));
        
        JButton btnEjecutar = new JButton("Ejecutar");
        btnEjecutar.setFocusPainted(false);
        btnEjecutar.setBackground(new Color(210, 210, 210));

        topPanel.add(menuBar);
        topPanel.add(btnEjecutar);
        add(topPanel, BorderLayout.NORTH);

        // panel del centro donde va el codigo
        JTabbedPane editorTabs = new JTabbedPane();
        codeEditor = new JTextArea();
        codeEditor.setFont(new Font("Monospaced", Font.PLAIN, 16));
        codeEditor.setText("var x int = 10;\nvar y float64 = 20.5;\n\nif x < y {\n    x = 5;\n    // descomenta la linea de abajo si tu interprete soporta print\n    // fmt.Println(\"x es menor, nuevo valor de x:\", x);\n} else {\n    y = 10.0;\n}");
        
        JScrollPane scrollEditor = new JScrollPane(codeEditor);
        editorTabs.addTab("main.glt", scrollEditor);
        editorTabs.addTab("funciones.glt", new JScrollPane(new JTextArea())); 
        
        add(editorTabs, BorderLayout.CENTER);

        // la consola negra de abajo
        JTabbedPane consoleTab = new JTabbedPane();
        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setBackground(new Color(40, 40, 40)); 
        consoleOutput.setForeground(new Color(220, 220, 220)); 
        consoleOutput.setFont(new Font("Monospaced", Font.PLAIN, 14));
        consoleOutput.setText("> lista para compilar...\n");

        JScrollPane scrollConsole = new JScrollPane(consoleOutput);
        scrollConsole.setPreferredSize(new Dimension(800, 200)); 
        
        consoleTab.addTab("Consola", scrollConsole);
        add(consoleTab, BorderLayout.SOUTH);

        // accion del boton para mandar a compilar
        btnEjecutar.addActionListener((ActionEvent e) -> {
            ejecutarCodigo();
        });

        // acciones de los reportes
        repTokens.addActionListener(e -> generarReporteTokens());
        repErrores.addActionListener(e -> generarReporteErrores());
        itemAbrir.addActionListener(e -> abrirArchivo());

        setLocationRelativeTo(null); 
    }

    private void ejecutarCodigo() {
        consoleOutput.setText("> ejecutando main.glt ...\n");
        consoleOutput.append("[info] compilacion iniciada\n\n");
        
        String codigo = codeEditor.getText();
        
        try {
            // creamos un archivo temporal seguro con ruta absoluta
            Path tempPath = Files.createTempFile("golite_temp", ".glt");
            Files.writeString(tempPath, codigo);

            // llamamos a la aplicacion principal
            GoLiteApplication app = new GoLiteApplication();
            ultimoResultado = app.run(tempPath);

            // borramos el archivo temporal para no dejar basura en la compu
            Files.deleteIfExists(tempPath);

            // imprimo la salida si es que el interpreter genero algo
            if (!ultimoResultado.output().isBlank()) {
                consoleOutput.append(ultimoResultado.output() + "\n");
            }

            // reviso si el collector atrapo algun error
            if (!ultimoResultado.errors().isEmpty()) {
                consoleOutput.append("\n[errores encontrados]\n");
                ultimoResultado.errors().forEach(error -> {
                    consoleOutput.append(error.type() + " L:" + error.line() + " C:" + error.column() + " -> " + error.message() + "\n");
                });
            } else {
                consoleOutput.append("\n[exito] analisis finalizado sin errores.\n");
            }

        } catch (Exception ex) {
            consoleOutput.append("\n[error] el sistema fallo al crear el archivo temporal: " + ex.getMessage() + "\n");
        }
    }

    private void generarReporteTokens() {
        if (ultimoResultado == null || ultimoResultado.tokens() == null || ultimoResultado.tokens().isEmpty()) {
            JOptionPane.showMessageDialog(this, "primero debes ejecutar el codigo para generar tokens.");
            return;
        }
        try {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Reporte de Tokens</title><style>table { border-collapse: collapse; width: 100%; font-family: sans-serif; } th, td { border: 1px solid #dddddd; text-align: left; padding: 8px; } th { background-color: #f2f2f2; }</style></head><body>");
            html.append("<h2>Reporte de Tokens</h2>");
            html.append("<table><tr><th>Linea</th><th>Columna</th><th>Tipo</th><th>Lexema</th></tr>");
            
            ultimoResultado.tokens().forEach(t -> {
                html.append("<tr><td>").append(t.line()).append("</td><td>").append(t.column())
                    .append("</td><td>").append(t.type()).append("</td><td>").append(t.lexeme()).append("</td></tr>");
            });
            
            html.append("</table></body></html>");
            Path path = Path.of("reporte_tokens.html");
            Files.writeString(path, html.toString());
            Desktop.getDesktop().browse(path.toUri()); 
        } catch (Exception ex) {
            consoleOutput.append("\n[error] no se pudo generar el reporte de tokens.\n");
        }
    }

    private void generarReporteErrores() {
        if (ultimoResultado == null || ultimoResultado.errors() == null || ultimoResultado.errors().isEmpty()) {
            JOptionPane.showMessageDialog(this, "ejecuta codigo que contenga errores primero para generar este reporte.");
            return;
        }
        try {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Reporte de Errores</title><style>table { border-collapse: collapse; width: 100%; font-family: sans-serif; } th, td { border: 1px solid #dddddd; text-align: left; padding: 8px; } th { background-color: #ffcccc; }</style></head><body>");
            html.append("<h2>Reporte de Errores</h2>");
            html.append("<table><tr><th>Tipo</th><th>Linea</th><th>Columna</th><th>Mensaje</th></tr>");
            
            ultimoResultado.errors().forEach(e -> {
                html.append("<tr><td>").append(e.type()).append("</td><td>").append(e.line())
                    .append("</td><td>").append(e.column()).append("</td><td>").append(e.message()).append("</td></tr>");
            });
            
            html.append("</table></body></html>");
            Path path = Path.of("reporte_errores.html");
            Files.writeString(path, html.toString());
            Desktop.getDesktop().browse(path.toUri()); 
        } catch (Exception ex) {
            consoleOutput.append("\n[error] no se pudo generar el reporte de errores.\n");
        }
    }
    private void abrirArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter("Archivos GoLite (*.glt)", "glt");
        fileChooser.setFileFilter(filter);

        int seleccion = fileChooser.showOpenDialog(this);

        if (seleccion == JFileChooser.APPROVE_OPTION) {
            java.io.File archivoSeleccionado = fileChooser.getSelectedFile();
            try {
                // Leemos todo el texto del archivo
                String contenido = new String(Files.readAllBytes(archivoSeleccionado.toPath()));
                
                // Lo inyectamos en tu editor principal
                codeEditor.setText(contenido);
                consoleOutput.append("> [info] Archivo cargado exitosamente: " + archivoSeleccionado.getName() + "\n");
                
            } catch (java.io.IOException ex) {
                consoleOutput.append("\n[error] No se pudo leer el archivo: " + ex.getMessage() + "\n");
            }
        }
    }
}