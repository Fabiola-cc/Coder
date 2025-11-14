package com.fmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fmd.modules.SemanticError;

import com.fmd.modules.TACInstruction;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.fmd.CompiscriptLexer;
import com.fmd.CompiscriptParser;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. Leer archivo de entrada
        String inputFile = args.length > 0 ? args[0] : "src\\main\\java\\com\\fmd\\program.cps";
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

        List<TACInstruction> tacInstructions = visitor_tac.getGenerator().getInstructions();

        if (tacInstructions == null || tacInstructions.isEmpty()) {
            System.out.println("⚠ No se generaron instrucciones TAC");
            return;
        }

        // Mostrar TAC
        System.out.println("\n RESULTADO TAC \n");
        for (int i = 0; i < tacInstructions.size(); i++) {
            TACInstruction tac = tacInstructions.get(i);
            System.out.printf("%3d: %s%n", i, tac.toString());
        }

        // 8. Generar código MIPS
        System.out.println("GENERACIÓN DE CÓDIGO MIPS");

        MIPSGenerator mipsGenerator = new MIPSGenerator(visitor_tac.getGenerator());

        // Agregar variables globales al segmento de datos si las hay
        mipsGenerator.addGlobalVariables();

        // Generar código MIPS
        String mipsCode = mipsGenerator.generate(tacInstructions);

        System.out.println(mipsCode);

        // 9. Guardar código MIPS en archivo
        String outputFile = inputFile.replace(".cps", ".asm");
        Files.writeString(Path.of(outputFile), mipsCode);

        System.out.println("Código MIPS generado exitosamente");
        System.out.println("Archivo guardado en: " + outputFile);
    }
}