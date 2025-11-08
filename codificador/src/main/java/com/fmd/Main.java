package com.fmd;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fmd.modules.SemanticError;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.fmd.CompiscriptLexer;
import com.fmd.CompiscriptParser;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. Leer archivo de entrada
        String inputFile = args.length > 0 ? args[0] : "codificador\\src\\main\\java\\com\\fmd\\program.cps";
        String code = Files.readString(Path.of(inputFile));

        System.out.println(" CÓDIGO FUENTE ");
        System.out.println(code);
        System.out.println();

        // 2. Crear lexer
        CompiscriptLexer lexer = new CompiscriptLexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // 3. Crear parser
        CompiscriptParser parser = new CompiscriptParser(tokens);

        // 4. Invocar la regla inicial
        ParseTree tree = parser.program();

        // 5. Análisis semántico
        System.out.println("\n ANÁLISIS SEMÁNTICO \n");
        SemanticVisitor visitor = new SemanticVisitor();
        visitor.visit(tree);

        visitor.getAllSymbols();

        // 6. Mostrar errores
        if (!visitor.getErrores().isEmpty()) {
            System.out.println("Se encontraron errores semánticos:");
            for (SemanticError err : visitor.getErrores()) {
                System.out.println(err);
            }
            System.out.println("¡¡¡ No se puede continuar con el TAC !!!");
            return;
        }
        System.out.println("✓ No hay errores semánticos");

        // 7. Generar TAC
        System.out.println("\n GENERACIÓN DE TAC \n");
        TACVisitor visitor_tac = new TACVisitor(visitor.getExistingScopes());
        visitor_tac.visit(tree);

        System.out.println("\n TABLA DE SÍMBOLOS ACTUALIZADA \n");
        visitor_tac.printTable();
    }
}