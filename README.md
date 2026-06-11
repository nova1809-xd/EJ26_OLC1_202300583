# golite interpreter

este repositorio contiene la base de la fase 1 del interprete de golite.

## alcance actual

- variables y tipos primitivos
- operaciones aritmeticas, logicas y relacionales
- if, if-else y for clasico
- break y continue
- funciones embebidas como fmt.Println y strconv.Atoi
- tabla de tokens y reporte de errores desde el inicio

## ejecucion

```bash
mvn exec:java -Dexec.args="ruta/al/archivo.golite"
```