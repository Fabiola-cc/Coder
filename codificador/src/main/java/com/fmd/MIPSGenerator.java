package com.fmd;

import java.util.*;
import com.fmd.modules.TACInstruction;
import com.fmd.modules.MIPSInstruction;
import com.fmd.modules.MIPSInstruction.OpCode;
import com.fmd.modules.Register;
import com.fmd.modules.Symbol;

/**
 * Generador de código MIPS MEJORADO
 *
 * MEJORAS sobre la versión original:
 * - Manejo de string literals en segmento de datos
 * - Eliminación de instrucciones redundantes (move $t0, $t0)
 * - Mejor manejo de acceso a arrays
 * - Segmento de datos más completo
 * - Inicialización y finalización del programa
 */
public class MIPSGenerator {
    private RegisterAllocator allocator;
    private List<MIPSInstruction> instructions;
    private Map<String, String> dataSegment;
    private Map<String, String> stringLiterals; // Mapea literales a labels
    private int labelCounter;
    private int stringCounter;
    private String currentFunction;
    private TACGenerator tacGenerator;

    private Map<String, List<MIPSInstruction>> scopeInstructions; // label -> instrucciones
    private Stack<String> scopeStack; // stack de labels activos
    private List<MIPSInstruction> globalInstructions; // código fuera de funciones/clases

    public MIPSGenerator(TACGenerator tacGenerator) {
        this.tacGenerator = tacGenerator;
        this.allocator = new RegisterAllocator(tacGenerator);
        this.instructions = new ArrayList<>();
        this.dataSegment = new LinkedHashMap<>();
        this.stringLiterals = new HashMap<>();
        this.labelCounter = 0;
        this.stringCounter = 0;
        this.currentFunction = null;

        this.scopeInstructions = new HashMap<>();
        this.scopeStack = new Stack<>();
        this.globalInstructions = new ArrayList<>();
    }

    /**
     * Genera código MIPS completo
     */
    public String generate(List<TACInstruction> tacList) {
        // PASO 1: Pre-procesar para encontrar string literals
        extractStringLiterals(tacList);

        // PASO 2: Generar instrucciones MIPS
        generateInstructions(tacList);

        // PASO 3: Ensamblar código completo
        StringBuilder code = new StringBuilder();
        code.append(generateDataSegment());
        code.append("\n");
        code.append(generateTextSegment());

        return code.toString();
    }

    /**
     * Extrae todos los string literals del TAC para ponerlos en .data
     */
    private void extractStringLiterals(List<TACInstruction> tacList) {
        for (TACInstruction tac : tacList) {
            checkAndAddString(tac.getArg1());
            checkAndAddString(tac.getArg2());
            checkAndAddString(tac.getResult());
        }
    }

    private void checkAndAddString(String value) {
        if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
            if (!stringLiterals.containsKey(value)) {
                String label = "str_" + stringCounter++;
                stringLiterals.put(value, label);

                // Eliminar comillas y escapar caracteres
                String content = value.substring(1, value.length() - 1);
                content = content.replace("\\n", "\\n")
                        .replace("\\t", "\\t")
                        .replace("\\\"", "\\\"");

                dataSegment.put(label, ".asciiz \"" + content + "\"");
            }
        }
    }

    /**
     * Genera el segmento de datos (.data)
     */
    /**
     * Genera el segmento de datos (.data) - VERSIÓN CORREGIDA
     */
    private String generateDataSegment() {
        StringBuilder data = new StringBuilder();
        data.append(".data\n");

        // 1. String literals del TAC
        for (Map.Entry<String, String> entry : dataSegment.entrySet()) {
            data.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // 2. Constantes y variables globales con valores LITERALES
        Map<String, Symbol> globalSymbols = getGlobalSymbols();
        if (globalSymbols != null && !globalSymbols.isEmpty()) {
            for (Symbol sym : globalSymbols.values()) {
                // Solo CONSTANTES y VARIABLES
                if (sym.getKind() != Symbol.Kind.VARIABLE &&
                        sym.getKind() != Symbol.Kind.CONSTANT) {
                    continue;
                }

                // Skip arrays (se manejan después)
                if (isArray(sym)) {
                    continue;
                }

                String literalValue = getLiteralInitialValue(sym);

                if (literalValue != null && !literalValue.isEmpty() &&
                        !isTemporalOrVariable(literalValue)) {

                    data.append(sym.getName()).append(": ");

                    String type = sym.getType() != null ? sym.getType().toLowerCase() : "integer";

                    switch (type) {
                        case "integer":
                        case "int":
                            try {
                                int value = Integer.parseInt(literalValue);
                                data.append(".word ").append(value);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                            break;
                        case "boolean":
                        case "bool":
                            int boolValue = literalValue.equals("true") ||
                                    literalValue.equals("1") ? 1 : 0;
                            data.append(".byte ").append(boolValue);
                            break;
                        default:
                            continue;
                    }
                    data.append("\n");
                }
            }
        }

        // 3. Arrays globales
        if (globalSymbols != null && !globalSymbols.isEmpty()) {
            for (Symbol sym : globalSymbols.values()) {
                if (sym.getKind() != Symbol.Kind.VARIABLE) {
                    continue;
                }

                if (isArray(sym)) {
                    int size = getArraySize(sym);

                    data.append(".align 2\n");

                    data.append(sym.getName()).append(": ");
                    data.append(".space ").append(size * 4);
                    data.append("  # Array de ").append(size).append(" elementos\n");
                }
            }
        }

        return data.toString();
    }

    /**
     * Obtiene el valor LITERAL inicial de un símbolo (solo constantes numéricas/booleanas)
     */
    private String getLiteralInitialValue(Symbol sym) {
        // Buscar la PRIMERA asignación en el TAC para este símbolo
        for (TACInstruction tac : tacGenerator.getInstructions()) {
            if (tac.getOp() == TACInstruction.OpType.ASSIGN &&
                    tac.getResult() != null &&
                    tac.getResult().equals(sym.getName())) {

                String value = tac.getArg1();

                // Solo retornar si es un LITERAL (número o booleano)
                if (value != null && !isTemporalOrVariable(value)) {
                    // Verificar si es número
                    try {
                        Integer.parseInt(value);
                        return value;
                    } catch (NumberFormatException e) {
                        // No es número, verificar si es booleano
                        if (value.equals("true") || value.equals("false") ||
                                value.equals("0") || value.equals("1")) {
                            return value;
                        }
                    }
                }

                // Si es temporal o variable, NO incluirlo en .data
                break;
            }
        }

        return null;
    }

    /**
     * Verifica si un valor es un temporal (t1, t2) o una variable
     */
    private boolean isTemporalOrVariable(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        // Es temporal si empieza con 't' seguido de números
        if (value.matches("^t\\d+$")) {
            return true;
        }

        // Es string literal si está entre comillas
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return false;
        }

        // Es número si se puede parsear
        try {
            Integer.parseInt(value);
            return false;
        } catch (NumberFormatException e) {
            // No es número
        }

        // Es booleano si es "true" o "false"
        if (value.equals("true") || value.equals("false")) {
            return false;
        }

        // Cualquier otra cosa es una variable
        return true;
    }

    /**
     * Obtiene los símbolos del scope global
     */
    private Map<String, Symbol> getGlobalSymbols() {
        try {
            SemanticVisitor.Entorno globalScope = tacGenerator.getScope("0");
            if (globalScope != null) {
                return globalScope.getSymbolsLocal();
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo símbolos globales: " + e.getMessage());
        }
        return new HashMap<>();
    }

    /**
     * Verifica si un símbolo es un array
     */
    private boolean isArray(Symbol sym) {
        return sym.getType() != null && sym.getType().endsWith("[]");
    }

    /**
     * Obtiene el tamaño total de un array desde Symbol
     */
    private int getArraySize(Symbol sym) {
        // Calcular desde el size del símbolo
        int totalSize = sym.getSize();

        // Si es integer[], cada elemento es 4 bytes
        // Si es integer[][], depende de las dimensiones
        String baseType = sym.getType().replace("[]", "");
        int elementSize = 4; // Por defecto

        if (baseType.equals("integer")) {
            elementSize = 4;
        } else if (baseType.equals("string")) {
            elementSize = 8;
        } else if (baseType.equals("boolean")) {
            elementSize = 1;
        }

        // Calcular número de elementos
        int elements = totalSize / elementSize;
        if (elements == 0) elements = 10; // Default si no se puede calcular

        return elements;
    }

    /**
     * Genera el segmento de texto (.text)
     */
    private String generateTextSegment() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n.text\n");
        sb.append(".globl main\n");
        sb.append("main:\n");
        sb.append("    move    $fp, $sp\n\n");

        // 1. Código global (main)
        for (MIPSInstruction instr : globalInstructions) {
            sb.append(instr.toString()).append("\n");
        }

        // Exit del programa principal
        sb.append("\n    # Fin del programa\n");
        sb.append("    li      $v0, 10\n");
        sb.append("    syscall\n\n");

        // 2. Todas las funciones y clases
        for (Map.Entry<String, List<MIPSInstruction>> entry : scopeInstructions.entrySet()) {
            for (MIPSInstruction instr : entry.getValue()) {
                sb.append(instr.toString()).append("\n");
            }
            sb.append("\n");
        }

        // 3. Runtime functions
        List<MIPSInstruction> tempInstructions = new ArrayList<>(instructions);
        instructions = new ArrayList<>();
        generateRuntimeFunctions();

        for (MIPSInstruction instr : instructions) {
            sb.append(instr.toString()).append("\n");
        }

        instructions = tempInstructions;

        return sb.toString();
    }

    /**
     * Genera todas las instrucciones MIPS
     */
    private void generateInstructions(List<TACInstruction> tacList) {
        instructions = globalInstructions; // Empezar en scope global

        for (TACInstruction tac : tacList) {
            allocator.advanceLine();

            // Detectar entrada a nuevo scope
            if (tac.getOp() == TACInstruction.OpType.LABEL_FUNCTION ||
                    tac.getOp() == TACInstruction.OpType.LABEL_CLASS) {
                Symbol currentSym = tacGenerator.getSymbol(tac.getLabel());
                String line = String.valueOf(currentSym.getLine());
                tacGenerator.setCurrentScopeLine(line);
                String label = tac.getLabel();
                scopeStack.push(label);
                scopeInstructions.put(label, new ArrayList<>());
                instructions = scopeInstructions.get(label); // Cambiar destino
            }

            generateInstruction(tac);

            // Detectar salida de scope
            if (tac.getOp() == TACInstruction.OpType.END ||
                    tac.getOp() == TACInstruction.OpType.END_CLASS) {
                if (!scopeStack.isEmpty()) {
                    scopeStack.pop();

                    // Volver al scope padre o global
                    if (scopeStack.isEmpty()) {
                        instructions = globalInstructions;
                    } else {
                        instructions = scopeInstructions.get(scopeStack.peek());
                    }
                }
            }
        }
    }

    /**
     * Genera instrucciones MIPS para un TAC
     */
    private void generateInstruction(TACInstruction tac) {
        TACInstruction.OpType opType = tac.getOp();

        switch (opType) {
            case ASSIGN:
                generateAssignment(tac);
                break;

            case BINARY_OP:
                generateArithmetic(tac);
                break;

            case UNARY_OP:
                generateUnary(tac);
                break;

            case LABEL_FUNCTION:
                generateFunctionProlog(tac);
                break;

            case LABEL_CLASS:
                instructions.add(MIPSInstruction.comment("Class " + tac.getLabel()));
                break;

            case LABEL:
                generateLabel(tac);
                break;

            case GOTO:
                generateGoto(tac);
                break;

            case IF_GOTO:
                generateIfGoto(tac);
                break;

            case CALL:
                generateCall(tac);
                break;

            case ASSIGN_CALL:
                generateAssignCall(tac);
                break;

            case NEW:
                generateNew(tac);
                break;

            case RETURN:
                generateReturn(tac);
                break;

            case END:
                generateFunctionEpilog(tac);
                break;

            case END_CLASS:
                instructions.add(MIPSInstruction.comment("end Class " + tac.getLabel()));
                break;

            case TRY_BEGIN:
                generateTryBegin(tac);
                break;

            case TRY_END:
                generateTryEnd(tac);
                break;

            default:
                instructions.add(MIPSInstruction.comment("Unsupported OpType: " + opType));
                break;
        }
    }

    /**
     * Genera asignación: x = y
     * MEJORADO: Maneja string literals, evita moves redundantes, arrays
     */
    private void generateAssignment(TACInstruction tac) {
        String dest = tac.getResult();
        String src = tac.getArg1();

        // CASO 1: String literal
        if (src != null && src.startsWith("\"") && src.endsWith("\"")) {
            String label = stringLiterals.get(src);
            if (label != null) {
                String destReg = allocator.getReg(dest);
                instructions.add(MIPSInstruction.la(destReg, label));
                allocator.markDirty(destReg);
            }
            return;
        }

        // CASO 2: Inmediato (constante numérica)
        if (isImmediate(src)) {
            String destReg = allocator.getReg(dest);
            int value = Integer.parseInt(src);
            instructions.add(MIPSInstruction.li(destReg, value));
            allocator.markDirty(destReg);
            return;
        }

        // CASO 3: Acceso a array (dest = arr[idx])
        if (src != null && src.contains("[") && src.contains("]")) {
            generateArrayLoad(dest, src);
            return;
        }

        // CASO 4: Asignación a array (arr[idx] = src)
        if (dest != null && dest.contains("[") && dest.contains("]")) {
            generateArrayStore(dest, src);
            return;
        }

        // CASO 5: Variable normal
        String destReg = allocator.getReg(dest);
        String srcReg = allocator.getReg(src);

        // MEJORA: Evitar move redundante (move $t0, $t0)
        if (!destReg.equals(srcReg)) {
            instructions.add(MIPSInstruction.move(destReg, srcReg));
        }

        allocator.markDirty(destReg);
    }

    /**
     * Carga desde array: dest = arr[idx]
     */
    /**
     * Genera código MIPS para cargar dest = arr[index]
     * arrayAccess: "numbers[10]" o "matrix[i]"
     * dest: nombre de la variable destino (puede ser temporal)
     */
    private void generateArrayLoad(String dest, String arrayAccess) {
        int openBracket = arrayAccess.indexOf('[');
        int closeBracket = arrayAccess.indexOf(']');

        if (openBracket < 0 || closeBracket < 0) return;

        String arrayName = arrayAccess.substring(0, openBracket);
        String index = arrayAccess.substring(openBracket + 1, closeBracket);

        Symbol arraySym = tacGenerator.getSymbol(arrayName);
        if (arraySym == null) {
            instructions.add(MIPSInstruction.comment("ERROR: Array " + arrayName + " not found"));
            return;
        }

        // Registro destino
        String destReg = allocator.getReg(dest);

        // Registro base para la dirección del array
        String baseReg = allocator.getReg("__arr_base_" + arrayName);
        int baseOffset = arraySym.getOffset();

        // 1) Obtener dirección base según sea local o global
        if (arraySym.isLocal()) {
            // LOCAL -> base = $fp + offset
            instructions.add(MIPSInstruction.typeI(
                    MIPSInstruction.OpCode.ADDI,
                    baseReg,
                    "$fp",
                    baseOffset
            ));
        } else {
            // GLOBAL -> la base (label)
            instructions.add(MIPSInstruction.la(
                    baseReg,
                    arraySym.getTacAddress()
            ));
        }

        // 2) Índice inmediato
        if (isImmediate(index)) {
            int idx = Integer.parseInt(index);
            int offset = idx * 4; // suponiendo elementSize = 4

            // lw destReg, offset(baseReg)
            instructions.add(MIPSInstruction.loadStore(
                    MIPSInstruction.OpCode.LW,
                    destReg,
                    offset + "(" + baseReg + ")"
            ));

            allocator.freeRegister(baseReg);
            allocator.markDirty(destReg);
            return;
        }

        // 3) Índice en registro
        String indexReg = allocator.getReg(index);
        String addrReg  = allocator.getReg("__arr_addr_" + arrayName);

        // offset = index * 4  -> sll addrReg, indexReg, 2
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.SLL,
                addrReg,    // rd
                indexReg,   // rs (we pass indexReg here)
                "2"         // rt (shamt as string, matches your typeR usage)
        ));

        // addrReg = baseReg + offset  -> add addrReg, baseReg, addrReg
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.ADD,
                addrReg,
                baseReg,
                addrReg
        ));

        // lw destReg, 0(addrReg)
        instructions.add(MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.LW,
                destReg,
                "0(" + addrReg + ")"
        ));

        // liberar temporales usados
        allocator.freeRegister(indexReg);
        allocator.freeRegister(addrReg);
        allocator.freeRegister(baseReg);

        allocator.markDirty(destReg);
    }


    /**
     * Almacena en array: arr[idx] = src
     */
    private void generateArrayStore(String arrayAccess, String src) {
        int openBracket = arrayAccess.indexOf('[');
        int closeBracket = arrayAccess.indexOf(']');

        if (openBracket < 0 || closeBracket < 0) return;

        String arrayName = arrayAccess.substring(0, openBracket);
        String index = arrayAccess.substring(openBracket + 1, closeBracket);

        Symbol arraySym = tacGenerator.getSymbol(arrayName);
        if (arraySym == null) {
            instructions.add(MIPSInstruction.comment("ERROR: Array " + arrayName + " not found"));
            return;
        }

        String srcReg = allocator.getReg(src);

        // === Base del array (registro) ===
        String baseReg = allocator.getReg("__arr_base_" + arrayName);

        int baseOffset = arraySym.getOffset();

        // ==================================================
        // 1. Obtener dirección base (local: FP+offset, global: la)
        // ==================================================
        if (arraySym.isLocal()) {
            // LOCAL: base = $fp + offset
            instructions.add(MIPSInstruction.typeI(
                    MIPSInstruction.OpCode.ADDI,
                    baseReg,
                    "$fp",
                    baseOffset
            ));
        } else {
            // GLOBAL: base = dirección global (label)
            instructions.add(MIPSInstruction.la(
                    baseReg,
                    arraySym.getTacAddress()
            ));
        }

        // ==================================================
        // 2. Caso índice inmediato
        // ==================================================
        if (isImmediate(index)) {
            int idx = Integer.parseInt(index);
            int offset = idx * 4;

            instructions.add(MIPSInstruction.loadStore(
                    MIPSInstruction.OpCode.SW,
                    srcReg,
                    offset + "(" + baseReg + ")"
            ));

            allocator.freeRegister(baseReg);
            return;
        }

        // ==================================================
        // 3. Caso índice en registro
        // ==================================================
        String indexReg = allocator.getReg(index);
        String addrReg  = allocator.getReg("__arr_addr_" + arrayName);

        // offset = index * 4   →  sll addrReg, indexReg, 2
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.SLL,
                addrReg,      // rd
                indexReg,     // rt (realmente es rt)
                "2"           // shamt
        ));

        // addrReg = base + offset → add addrReg, baseReg, addrReg
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.ADD,
                addrReg,
                baseReg,
                addrReg
        ));

        // Guardar valor: sw srcReg, 0(addrReg)
        instructions.add(MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.SW,
                srcReg,
                "0(" + addrReg + ")"
        ));

        allocator.freeRegister(indexReg);
        allocator.freeRegister(addrReg);
        allocator.freeRegister(baseReg);
    }




    /**
     * Genera operaciones aritméticas: x = y op z
     */
    private void generateArithmetic(TACInstruction tac) {
        String result = tac.getResult();
        String arg1 = tac.getArg1();
        String arg2 = tac.getArg2();
        String op = tac.getOperator();

        String resultReg = allocator.getReg(result);
        String arg1Reg = allocator.getReg(arg1);

        OpCode opCode = getMipsArithmeticOp(op);

        if (isImmediate(arg2)) {
            int immediate = Integer.parseInt(arg2);

            if (op.equals("+")) {
                instructions.add(MIPSInstruction.typeI(OpCode.ADDI, resultReg, arg1Reg, immediate));
            } else if (op.equals("-")) {
                instructions.add(MIPSInstruction.typeI(OpCode.ADDI, resultReg, arg1Reg, -immediate));
            } else {
                String tempReg = allocator.getReg("temp_imm");
                instructions.add(MIPSInstruction.li(tempReg, immediate));
                instructions.add(MIPSInstruction.typeR(opCode, resultReg, arg1Reg, tempReg));
                allocator.freeRegister(tempReg);
            }
        } else {
            String arg2Reg = allocator.getReg(arg2);
            instructions.add(MIPSInstruction.typeR(opCode, resultReg, arg1Reg, arg2Reg));
        }

        allocator.markDirty(resultReg);
    }

    /**
     * Genera operaciones unarias
     */
    private void generateUnary(TACInstruction tac) {
        String result = tac.getResult();
        String arg = tac.getArg1();
        String op = tac.getOperator();

        String resultReg = allocator.getReg(result);
        String argReg = allocator.getReg(arg);

        if (op.equals("-")) {
            instructions.add(MIPSInstruction.typeR(OpCode.SUB, resultReg, Register.ZERO.getName(), argReg));
        } else if (op.equals("!")) {
            instructions.add(MIPSInstruction.typeR(OpCode.SEQ, resultReg, argReg, Register.ZERO.getName()));
        } else {
            instructions.add(MIPSInstruction.comment("Unknown unary operator: " + op));
        }

        allocator.markDirty(resultReg);
    }

    /**
     * Genera goto incondicional
     */
    private void generateGoto(TACInstruction tac) {
        String label = tac.getLabel();
        instructions.add(MIPSInstruction.jump(OpCode.J, label));
    }

    /**
     * Genera if condicional: if x relop y goto label
     */
    private void generateIfGoto(TACInstruction tac) {
        String arg1 = tac.getArg1();
        String arg2 = tac.getArg2();
        String relop = tac.getRelop();
        String label = tac.getLabel();

        String arg1Reg = allocator.getReg(arg1);

        // MEJORA: Si arg2 es inmediato, cargarlo a registro
        String arg2Reg;
        boolean needsFree = false;

        if (isImmediate(arg2)) {
            arg2Reg = allocator.getReg("temp_cmp");
            instructions.add(MIPSInstruction.li(arg2Reg, Integer.parseInt(arg2)));
            needsFree = true;
        } else {
            arg2Reg = allocator.getReg(arg2);
        }

        OpCode branchOp = getMipsComparisonBranchOp(relop);
        instructions.add(MIPSInstruction.branch(branchOp, arg1Reg, arg2Reg, label));

        if (needsFree) {
            allocator.freeRegister(arg2Reg);
        }
    }

    /**
     * Genera llamada a función sin asignación
     */
    private void generateCall(TACInstruction tac) {
        String functionName = tac.getArg1();
        List<String> params = tac.getParams();

        if (functionName.equals("print") && !params.isEmpty()) {
            String param = params.get(0);
            String paramReg = allocator.getReg(param);

            // Mover parámetro a $a0
            instructions.add(MIPSInstruction.move("$a0", paramReg));

            // Intentar detectar el tipo
            Symbol paramSym = tacGenerator.getSymbol(param);
            if (paramSym != null && paramSym.getType() != null) {
                String type = paramSym.getType();

                if (type.equals("integer")) {
                    // Print directo de entero
                    instructions.add(MIPSInstruction.li("$v0", 1));
                    instructions.add(MIPSInstruction.syscall());
                    return;
                } else if (type.equals("string")) {
                    // Print directo de string
                    instructions.add(MIPSInstruction.li("$v0", 4));
                    instructions.add(MIPSInstruction.syscall());
                    return;
                }
            }

            // Si no sabemos el tipo, llamar a print genérica
            allocator.saveTemporaries();
            instructions.add(MIPSInstruction.jump(OpCode.JAL, "print"));
            return;
        }

        if (functionName.equals("read") || functionName.equals("read_int")) {
            allocator.saveTemporaries();
            instructions.add(MIPSInstruction.jump(OpCode.JAL, "read_int"));
            return;
        }

        // Para otras funciones, comportamiento normal
        generateParameters(params);
        allocator.saveTemporaries();
        instructions.add(MIPSInstruction.jump(OpCode.JAL, functionName));
    }

    /**
     * Genera llamada a función con asignación
     */
    private void generateAssignCall(TACInstruction tac) {
        String result = tac.getResult();
        String functionName = tac.getArg1();
        List<String> params = tac.getParams();

        generateParameters(params);
        allocator.saveTemporaries();
        instructions.add(MIPSInstruction.jump(OpCode.JAL, functionName));

        String resultReg = allocator.getReg(result);
        instructions.add(MIPSInstruction.move(resultReg, Register.V0.getName()));
        allocator.markDirty(resultReg);
    }

    /**
     * Genera parámetros usando objetos Register
     */
    private void generateParameters(List<String> params) {
        Register[] argRegs = {Register.A0, Register.A1, Register.A2, Register.A3};

        for (int i = 0; i < params.size(); i++) {
            String param = params.get(i);
            String paramReg = allocator.getReg(param);

            if (i < 4) {
                instructions.add(MIPSInstruction.move(argRegs[i].getName(), paramReg));
            } else {
                int offset = (i - 4) * 4;
                instructions.add(MIPSInstruction.loadStore(OpCode.SW, paramReg, offset + "($sp)"));
            }
        }
    }

    /**
     * Genera creación de objeto
     */
    private void generateNew(TACInstruction tac) {
        String result = tac.getResult();
        String className = tac.getArg1();
        List<String> params = tac.getParams();

        instructions.add(MIPSInstruction.comment("new " + className));
        generateParameters(params);
        instructions.add(MIPSInstruction.jump(OpCode.JAL, className + "_constructor"));

        String resultReg = allocator.getReg(result);
        instructions.add(MIPSInstruction.move(resultReg, Register.V0.getName()));
        allocator.markDirty(resultReg);
    }

    /**
     * Genera return
     */
    private void generateReturn(TACInstruction tac) {
        String returnValue = tac.getArg1();

        if (returnValue != null && !returnValue.isEmpty() && !returnValue.equals("null")) {
            String returnReg = allocator.getReg(returnValue);
            instructions.add(MIPSInstruction.move(Register.V0.getName(), returnReg));
        }

        if (currentFunction != null) {
            instructions.add(MIPSInstruction.jump(OpCode.J, currentFunction + "_epilog"));
        } else {
            instructions.add(MIPSInstruction.jump(OpCode.J, "epilog"));
        }
    }

    /**
     * Genera etiqueta
     */
    private void generateLabel(TACInstruction tac) {
        String label = tac.getLabel();
        instructions.add(MIPSInstruction.label(label));
    }

    /**
     * Genera prólogo de función
     */
    private void generateFunctionProlog(TACInstruction tac) {
        String functionName = tac.getLabel();
        currentFunction = functionName;

        instructions.add(MIPSInstruction.label(functionName));

        int localSpace = calculateLocalSpace();
        int frameSize = 8 + localSpace;

        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, Register.SP.getName(), Register.SP.getName(), -frameSize));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, Register.FP.getName(), (frameSize - 8) + "(" + Register.SP.getName() + ")"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, Register.RA.getName(), (frameSize - 4) + "(" + Register.SP.getName() + ")"));
        instructions.add(MIPSInstruction.move(Register.FP.getName(), Register.SP.getName()));

        allocator.reset();
    }

    /**
     * Genera epílogo de función
     */
    private void generateFunctionEpilog(TACInstruction tac) {
        String functionName = tac.getLabel();
        instructions.add(MIPSInstruction.label(functionName + "_epilog"));

        int localSpace = calculateLocalSpace();
        int frameSize = 8 + localSpace;

        allocator.flushAll();

        instructions.add(MIPSInstruction.move(Register.SP.getName(), Register.FP.getName()));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, Register.FP.getName(), (frameSize - 8) + "(" + Register.SP.getName() + ")"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, Register.RA.getName(), (frameSize - 4) + "(" + Register.SP.getName() + ")"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, Register.SP.getName(), Register.SP.getName(), frameSize));
        instructions.add(MIPSInstruction.jumpReg(Register.RA.getName()));

        currentFunction = null;
    }

    /**
     * Genera try begin
     */
    private void generateTryBegin(TACInstruction tac) {
        String catchLabel = tac.getLabel();
        instructions.add(MIPSInstruction.comment("try_begin -> catch: " + catchLabel));
    }

    /**
     * Genera try end
     */
    private void generateTryEnd(TACInstruction tac) {
        instructions.add(MIPSInstruction.comment("try_end"));
    }

    /**
     * Verifica si es inmediato
     */
    private boolean isImmediate(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Obtiene opcode aritmético
     */
    private OpCode getMipsArithmeticOp(String op) {
        switch (op) {
            case "+": return OpCode.ADD;
            case "-": return OpCode.SUB;
            case "*": return OpCode.MUL;
            case "/": return OpCode.DIV;
            default: return OpCode.ADD;
        }
    }

    /**
     * Obtiene opcode de branch
     */
    private OpCode getMipsComparisonBranchOp(String op) {
        switch (op) {
            case "<": return OpCode.BLT;
            case ">": return OpCode.BGT;
            case "<=": return OpCode.BLE;
            case ">=": return OpCode.BGE;
            case "==": return OpCode.BEQ;
            case "!=": return OpCode.BNE;
            default: return OpCode.BEQ;
        }
    }

    /**
     * Genera etiqueta temporal única
     */
    private String generateTempLabel() {
        return "L" + (labelCounter++);
    }

    /**
     * Calcula espacio necesario para variables locales
     */
    private int calculateLocalSpace() {
        if (currentFunction == null) return 0;
        Symbol funcSym = tacGenerator.getSymbol(currentFunction);

        // Resultado del tamaño de todas las variables locales
        return funcSym.getLocalVarSize();
    }

    /**
     * Agrega variable al segmento de datos
     */
    public void addDataVariable(String name, String value) {
        dataSegment.put(name, value);
    }

    /**
     * Obtiene instrucciones generadas
     */
    public List<MIPSInstruction> getInstructions() {
        List<MIPSInstruction> allInstructions = new ArrayList<>();
        allInstructions.addAll(instructions);
        allInstructions.addAll(allocator.getInstructions());
        return allInstructions;
    }

    /**
     * Obtiene allocator
     */
    public RegisterAllocator getAllocator() {
        return allocator;
    }

    /**
     * Obtiene contador de registros usados
     */
    public int getUsedRegistersCount() {
        int count = 0;
        allocator.printState();
        return count;
    }
    private void generatePrintBoolFunction() {
        instructions.add(MIPSInstruction.label("print_bool"));

        // Si $a0 == 0, imprimir "false"
        instructions.add(MIPSInstruction.branch(OpCode.BEQ, "$a0", "$zero", "print_bool_false"));

        // Imprimir "true"
        instructions.add(MIPSInstruction.la("$a0", "true_str"));
        instructions.add(MIPSInstruction.li("$v0", 4));
        instructions.add(MIPSInstruction.syscall());
        instructions.add(MIPSInstruction.jump(OpCode.J, "print_bool_end"));

        // Imprimir "false"
        instructions.add(MIPSInstruction.label("print_bool_false"));
        instructions.add(MIPSInstruction.la("$a0", "false_str"));
        instructions.add(MIPSInstruction.li("$v0", 4));
        instructions.add(MIPSInstruction.syscall());

        instructions.add(MIPSInstruction.label("print_bool_end"));
        instructions.add(MIPSInstruction.jumpReg("$ra"));
    }
// ============================================
// RUNTIME FUNCTIONS - AGREGAR AL FINAL DE LA CLASE
// ============================================

    /**
     * Genera todas las funciones de runtime (LLAMAR desde generate())
     */
    private void generateRuntimeFunctions() {
        generatePrintFunction();
        generateReadIntFunction();
        generatePrintBoolFunction();
        generatePrintNewlineFunction();
    }

    private void generatePrintFunction() {
        instructions.add(MIPSInstruction.label("print"));

        // Guardar $ra
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", -4));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$ra", "0($sp)"));

        // Detectar si es puntero (string) o valor (int)
        instructions.add(MIPSInstruction.li("$t9", 0x10000000));
        instructions.add(MIPSInstruction.branch(OpCode.BGE, "$a0", "$t9", "print_as_string"));

        // Imprimir como int
        instructions.add(MIPSInstruction.label("print_as_int"));
        instructions.add(MIPSInstruction.li("$v0", 1));
        instructions.add(MIPSInstruction.syscall());
        instructions.add(MIPSInstruction.jump(OpCode.J, "print_end"));

        // Imprimir como string
        instructions.add(MIPSInstruction.label("print_as_string"));
        instructions.add(MIPSInstruction.li("$v0", 4));
        instructions.add(MIPSInstruction.syscall());

        // Restaurar y retornar
        instructions.add(MIPSInstruction.label("print_end"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$ra", "0($sp)"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", 4));
        instructions.add(MIPSInstruction.jumpReg("$ra"));
    }

    private void generateReadIntFunction() {
        instructions.add(MIPSInstruction.label("read_int"));
        instructions.add(MIPSInstruction.li("$v0", 5));
        instructions.add(MIPSInstruction.syscall());
        instructions.add(MIPSInstruction.jumpReg("$ra"));
    }

    private void generatePrintNewlineFunction() {
        instructions.add(MIPSInstruction.label("print_newline"));
        instructions.add(MIPSInstruction.la("$a0", "newline"));
        instructions.add(MIPSInstruction.li("$v0", 4));
        instructions.add(MIPSInstruction.syscall());
        instructions.add(MIPSInstruction.jumpReg("$ra"));
    }
    /**
     * Agrega variables globales al segmento de datos
     */
    public void addGlobalVariables() {
        addDataVariable("newline", ".asciiz \"\\n\"");
        addDataVariable("space", ".asciiz \" \"");
        addDataVariable("true_str", ".asciiz \"true\"");
        addDataVariable("false_str", ".asciiz \"false\"");
    }
}