package com.fmd;

import com.fmd.modules.TACInstruction;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public class TestInit {

    public String generateMIPS(String code) {
        // Lexer y parser
        CompiscriptLexer lexer = new CompiscriptLexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CompiscriptParser parser = new CompiscriptParser(tokens);
        ParseTree tree = parser.program();

        // Semántico
        SemanticVisitor visitor = new SemanticVisitor();
        visitor.visit(tree);

        // Generación TAC
        TACVisitor visitor_tac = new TACVisitor(visitor.getExistingScopes());
        visitor_tac.visit(tree);
        List<TACInstruction> tacInstructions = visitor_tac.getGenerator().getInstructions();

        // Generación MIPS
        MIPSGenerator mipsGenerator = new MIPSGenerator(visitor_tac.getGenerator());
        // No genera la sección de .data para comparaciones más simples

        return mipsGenerator.generate(tacInstructions);
    }

    /**
     * Cuenta instrucciones MIPS generadas (sin contar etiquetas ni comentarios)
     */
    public int countMIPSInstructions(String mips) {
        if (mips == null) return 0;

        int count = 0;
        String[] lines = mips.split("\n");

        for (String line : lines) {
            line = line.trim();

            // Ignorar líneas vacías, etiquetas, directivas y comentarios
            if (line.isEmpty() ||
                    line.startsWith(".") ||
                    line.endsWith(":") ||
                    line.startsWith("#")) {
                continue;
            }

            count++;
        }

        return count;
    }
}
