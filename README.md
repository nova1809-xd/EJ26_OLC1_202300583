# Intérprete GoLite - Fase 2

Este proyecto es un entorno de desarrollo e intérprete para el lenguaje de programación **GoLite**, un subconjunto inspirado en el lenguaje Go, diseñado para aplicar los conceptos fundamentales de la teoría de compiladores. 

Desarrollado para el curso de Organización de Lenguajes y Compiladores 1, Escuela de Vacaciones Junio 2026.

## Características Principales
* **Análisis Léxico y Sintáctico:** Generados dinámicamente mediante las herramientas JFlex y CUP.
* **Interfaz Gráfica (IDE):** Editor de texto integrado con soporte para múltiples pestañas, apertura y guardado de archivos `.glt`.
* **Análisis Semántico y Ejecución:** Construcción de un Árbol de Sintaxis Abstracta (AST) para la evaluación de tipos, validación de variables, manejo de referencias (structs/slices) y control de flujo.
* **Recuperación de Errores (Panic Mode):** El intérprete atrapa errores en tiempo de ejecución (ej. tipos incompatibles o nulos) y continúa la evaluación para generar un reporte completo.

## Cómo descargar y ejecutar el proyecto

Para facilitar su uso, el proyecto ha sido empaquetado en un archivo ejecutable `.jar`. No es necesario compilar el código fuente para probar la aplicación.

### Requisitos previos
* Java Runtime Environment (JRE) versión 21 o superior.

### Pasos de ejecución
1. Dirígete a la sección de **Releases** en este repositorio de GitHub.
2. Descarga el archivo `GoLiteCompiler.jar` adjunto en los *Assets* de la última versión.
3. Para ejecutar la aplicación, puedes hacer doble clic sobre el archivo `.jar` descargado, o ejecutar el siguiente comando en tu terminal:
   ```bash
   java -jar GoLiteCompiler.jar