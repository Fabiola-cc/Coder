package com.fmd;

import com.fmd.modules.TACInstruction;
import org.springframework.boot.SpringApplication;
import org.springframework.core.io.Resource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import com.fmd.modules.SemanticError;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.fmd.CompiscriptLexer;
import com.fmd.CompiscriptParser;
import com.fmd.CompiscriptBaseVisitor;

@SpringBootApplication
public class MIPSApp {
    public static void main(String[] args) {
        SpringApplication.run(MIPSApp.class, args);
    }
}

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/")
class MIPSController {
    String outputFile = "codificador\\src\\main\\java\\com\\fmd\\program.asm";

    @PostMapping("/compilar-mips")
    public Map<String, Object> analizar(@RequestBody Map<String, String> body) throws Exception {
        String code = body.get("codigo");
        Map<String, Object> response = new HashMap<>();

        // 1. Crear lexer y parser
        CompiscriptLexer lexer = new CompiscriptLexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CompiscriptParser parser = new CompiscriptParser(tokens);

        // 2. Parsear
        ParseTree tree = parser.program();

        // 3. Semántico
        SemanticVisitor visitor = new SemanticVisitor();
        visitor.visit(tree);

        // 4. Guardar errores
        List<SemanticError> errores = visitor.getErrores();

        // 5. Ejecutar script de Python para generar imagen del árbol
        String treeString = tree.toStringTree(parser);
        String base64Img = generarImagen(treeString);

        // Retornar si hay errores semánticos
        if (!errores.isEmpty()) {
            List<Map<String, Object>> simbolos = visitor.getRaiz().getAllScopesSymbols().values().stream()
                    .map(sym -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", sym.getName());
                        map.put("type", sym.getType());
                        map.put("kind", sym.getKind());
                        map.put("line", sym.getLine());
                        map.put("column", sym.getColumn());
                        return map;
                    })
                    .toList();

            response.put("errors", errores);
            response.put("symbols", simbolos);
            response.put("astImage", base64Img);
            response.put("tac",Collections.singletonList("Hay errores semánticos, no se puede continuar con el TAC"));

            return response;
        }

        // 6. Ejecutar generador de código intermedio
        System.out.println("\n GENERACIÓN DE TAC \n");
        TACVisitor visitor_tac = new TACVisitor(visitor.getExistingScopes());
        visitor_tac.visit(tree);

        List<Map<String, Object>> simbolos = visitor_tac.getGenerator().getScope("0").getAllScopesSymbols().values().stream()
                .map(sym -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", sym.getName());
                    map.put("type", sym.getType());
                    map.put("kind", sym.getKind());
                    map.put("line", sym.getLine());
                    map.put("column", sym.getColumn());
                    map.put("offset", sym.getOffset());
                    map.put("size", sym.getSize());
                    return map;
                })
                .toList();

        List<TACInstruction> tacInstructions = visitor_tac.getGenerator().getInstructions();
        if (tacInstructions == null || tacInstructions.isEmpty()) {
            response.put("errors", errores);
            response.put("symbols", simbolos);
            response.put("astImage", base64Img);
            response.put("tac",Collections.singletonList("⚠ No se generaron instrucciones TAC"));

            return response;
        }

        List<String> instructions = tacInstructions.stream()
                .map(Object::toString)
                .toList();

        // 7. Ejecutar generador de código MIPS
        MIPSGenerator mipsGenerator = new MIPSGenerator(visitor_tac.getGenerator());

        mipsGenerator.addGlobalVariables(); // Agregar variables globales al segmento de datos si las hay
        String mipsCode = mipsGenerator.generate(tacInstructions); // Generar código MIPS

        // 8. Guardar código MIPS en archivo
        Files.writeString(Path.of(outputFile), mipsCode);

        System.out.println("Código MIPS generado exitosamente");
        System.out.println("Archivo guardado en: " + outputFile);

        response.put("errors", errores);
        response.put("symbols", simbolos);
        response.put("astImage", base64Img);
        response.put("tac", instructions);

        return response;
    }

    @GetMapping("download/mips")
    public ResponseEntity<Resource> downloadMips() throws IOException {
        File file = new File(outputFile);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=program.asm")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }

    public static String generarImagen(String treeString) {
        try {
            // Ejecuta un script Python que recibe el árbol y devuelve Base64
            ProcessBuilder pb = new ProcessBuilder("python", "additions\\AstTreeGenerator.py");
            Process p = pb.start();

            // Mandar el árbol al script
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()))) {
                writer.write(treeString);
                writer.newLine();
                writer.flush();
            }

            // Recibir errores
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println("[PYTHON ERROR] " + line);
            }

            // Esperar a que termine
            p.waitFor();

            // Leer el archivo generado
            byte[] bytes = Files.readAllBytes(Paths.get("ast_tree.png"));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
