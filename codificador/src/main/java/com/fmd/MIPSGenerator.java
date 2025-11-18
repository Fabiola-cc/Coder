package com.fmd;

import java.util.*;
import com.fmd.modules.TACInstruction;
import com.fmd.modules.MIPSInstruction;
import com.fmd.modules.MIPSInstruction.OpCode;
import com.fmd.modules.Register;
import com.fmd.modules.Symbol;
import jakarta.servlet.ServletOutputStream;

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
    private Map<String, String> tempTypeMap;

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
        this.tempTypeMap = new HashMap<>();
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
                // Detectar si es property access o property set
                String dest = tac.getResult();
                String src = tac.getArg1();

                if (dest != null && dest.contains(".")) {
                    // Property set: obj.prop = value
                    generatePropertySet(tac);
                } else if (src != null && src.contains(".")) {
                    // Property get: var = obj.prop
                    generatePropertyGet(tac);
                } else {
                    generateAssignment(tac);
                }
                break;

            case BINARY_OP:
                generateBinaryOp(tac);
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
                // Detectar si es method call
                String functionName = tac.getArg1();
                if (functionName != null && functionName.contains(".")) {
                    generateMethodCall(tac);
                } else {
                    generateCall(tac);
                }
                break;

            case ASSIGN_CALL:
                // Detectar si es method call con asignación
                String funcName = tac.getArg1();
                if (funcName != null && funcName.contains(".")) {
                    generateMethodCallWithReturn(tac);
                } else {
                    generateAssignCall(tac);
                }
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

    private void generatePropertyGet(TACInstruction tac) {
        String dest = tac.getResult();
        String propertyAccess = tac.getArg1(); // "p.edad"

        int dotIndex = propertyAccess.indexOf('.');
        String objName = propertyAccess.substring(0, dotIndex);
        String propName = propertyAccess.substring(dotIndex + 1);

        instructions.add(MIPSInstruction.comment("Property get: " + dest + " = " + propertyAccess));

        // Obtener registro del objeto
        String objReg;
        if (objName.equals("this")) {
            objReg = "$a0";
        } else {
            // CRÍTICO: Usar el registro correcto del allocator
            objReg = allocator.getReg(objName);
        }

        // Obtener offset de la propiedad
        Symbol objSymbol = getObjectSymbol(objName);
        if (objSymbol == null) {
            instructions.add(MIPSInstruction.comment("ERROR: Object symbol not found: " + objName));
            return;
        }

        int propOffset = getPropertyOffsetWithInheritance(objSymbol, propName);

        // Cargar propiedad
        String destReg = allocator.getReg(dest);
        instructions.add(MIPSInstruction.loadStore(
                OpCode.LW,
                destReg,
                propOffset + "(" + objReg + ")"
        ));

        allocator.markDirty(destReg);
    }

    private void generatePropertySet(TACInstruction tac) {
        String propertyAccess = tac.getResult();
        String value = tac.getArg1();

        int dotIndex = propertyAccess.indexOf('.');
        String objName = propertyAccess.substring(0, dotIndex);
        String propName = propertyAccess.substring(dotIndex + 1);

        instructions.add(MIPSInstruction.comment("Property set: " + propertyAccess + " = " + value));

        // 1. Cargar THIS desde el stack frame
        String objReg = "$t8";
        if (objName.equals("this")) {
            instructions.add(MIPSInstruction.loadStore(OpCode.LW, objReg, "0($fp)")); // ← CAMBIO: usar $fp
        } else {
            objReg = allocator.getReg(objName);
        }

        // 2. Cargar VALUE desde el stack frame
        String valueReg = "$t9";
        Symbol valueSym = tacGenerator.getSymbol(value);

        if (valueSym != null && valueSym.isLocal()) {
            Symbol funcSym = tacGenerator.getSymbol(currentFunction);
            if (funcSym != null) {
                List<Symbol> params = funcSym.getParams();
                int paramIndex = -1;

                for (int i = 0; i < params.size(); i++) {
                    if (params.get(i).getName().equals(value)) {
                        paramIndex = i;
                        break;
                    }
                }

                if (paramIndex >= 0) {
                    // CORRECCIÓN: Parámetros están en offsets relativos a $fp
                    // Si es método: 0($fp)=this, 4($fp)=param0, 8($fp)=param1
                    // Si es función: 0($fp)=param0, 4($fp)=param1
                    boolean isMethod = funcSym.getEnclosingClassName() != null;
                    int offset;

                    if (isMethod) {
                        offset = (paramIndex + 1) * 4; // +1 porque this está en 0($fp)
                    } else {
                        offset = paramIndex * 4;
                    }

                    instructions.add(MIPSInstruction.loadStore(OpCode.LW, valueReg, offset + "($fp)")); // ← CAMBIO: usar $fp
                    instructions.add(MIPSInstruction.comment("Loaded param " + value + " from " + offset + "($fp) into " + valueReg));
                } else {
                    instructions.add(MIPSInstruction.comment("ERROR: Parameter index not found for " + value));
                    valueReg = allocator.getReg(value);
                }
            }
        } else if (isImmediate(value)) {
            instructions.add(MIPSInstruction.li(valueReg, Integer.parseInt(value)));
        } else if (isStringLiteral(value)) {
            String label = stringLiterals.get(value);
            instructions.add(MIPSInstruction.la(valueReg, label));
        } else {
            valueReg = allocator.getReg(value);
        }

        // 3. Obtener offset de la propiedad
        Symbol objSymbol = getObjectSymbol(objName);
        if (objSymbol == null) {
            instructions.add(MIPSInstruction.comment("ERROR: Object symbol not found: " + objName));
            return;
        }

        int propOffset = getPropertyOffsetWithInheritance(objSymbol, propName);

        // 4. Guardar propiedad
        instructions.add(MIPSInstruction.loadStore(
                OpCode.SW,
                valueReg,
                propOffset + "(" + objReg + ")"
        ));
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
        String srcReg = allocator.getReg(src);
        String destReg = "";

        if(srcReg.startsWith("$s")){ // CASO 5.1: Asignación de clases
            destReg = allocator.getRegAssign(dest, src);
        } else {
            destReg = allocator.getReg(dest);
        }

        // Evitar move redundante (move $t0, $t0)
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
     * DISPATCHER PRINCIPAL para operaciones binarias
     * Detecta el tipo de operación y delega al generador apropiado
     *
     * Casos:
     * 1. Operadores relacionales (<, >, ==, etc.) → Comparación
     * 2. Operadores aritméticos con strings (+) → Concatenación
     * 3. Operadores aritméticos normales (+, -, *, /) → Aritmética
     * 4. Operadores lógicos (&&, ||) → Ya manejados en TAC
     */
    private void generateBinaryOp(TACInstruction tac) {
        String op = tac.getOperator();
        String arg1 = tac.getArg1();
        String arg2 = tac.getArg2();

        System.out.println(arg1 + " " + op + " " + arg2);

        // CASO 1: Operadores de comparación/relacionales
        if (isComparisonOperator(op)) {
            System.out.println("IS COMPARISON");
            generateComparison(tac);
            return;
        }

        // CASO 2: Concatenación de strings (solo con +)
        if (op.equals("+") && isStringOperation(arg1, arg2)) {
            System.out.println("IS STRING OPERATION");
            generateStringConcatenation(tac);
            return;
        }

        // CASO 3: Operadores aritméticos normales
        if (isArithmeticOperator(op)) {
            System.out.println("IS ARITHMETIC OPERATION");
            generateArithmetic(tac);
            return;
        }

        // CASO 4: Operador no soportado
        instructions.add(MIPSInstruction.comment("Unsupported binary operator: " + op));
    }

    /**
     * Verifica si es un operador de comparación
     */
    private boolean isComparisonOperator(String op) {
        return op.equals("<") || op.equals(">") || op.equals("<=") ||
                op.equals(">=") || op.equals("==") || op.equals("!=");
    }

    /**
     * Verifica si es un operador aritmético
     */
    private boolean isArithmeticOperator(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") ||
                op.equals("/") || op.equals("%");
    }

    private boolean isStringOperation(String arg1, String arg2) {
        // Caso 1: Literal de string directo
        if (isStringLiteral(arg1) || isStringLiteral(arg2)) {
            return true;
        }

        // Caso 2: Variable con tipo string conocido EN LA TABLA DE SÍMBOLOS
        Symbol sym1 = tacGenerator.getSymbol(arg1);
        Symbol sym2 = tacGenerator.getSymbol(arg2);

        //  VERIFICAR TIPO EXPLÍCITO (no rastrear temporales)
        if (sym1 != null && sym1.getType() != null && sym1.getType().equals("string")) {
            return true;
        }
        if (sym2 != null && sym2.getType() != null && sym2.getType().equals("string")) {
            return true;
        }

        //  NUEVO: Solo rastrear temporales si tienen asignación DIRECTA de string
        if (isTemporalWithDirectStringAssignment(arg1) || isTemporalWithDirectStringAssignment(arg2)) {
            return true;
        }

        return false;
    }

    /**
     * Verifica si un temporal fue asignado DIRECTAMENTE desde un string literal
     * Busca HACIA ATRÁS desde la línea actual del allocator
     */
    private boolean isTemporalWithDirectStringAssignment(String temporal) {
        if (temporal == null || !temporal.matches("^t\\d+$")) {
            return false;
        }

        // CRÍTICO: Buscar solo hasta la línea actual
        int searchLimit = Math.min(allocator.getCurrentLine(), tacGenerator.getInstructions().size());

        // Buscar la ÚLTIMA asignación ANTES de la línea actual
        for (int i = searchLimit - 1; i >= 0; i--) {
            TACInstruction tac = tacGenerator.getInstructions().get(i);

            if (tac.getOp() == TACInstruction.OpType.ASSIGN &&
                    tac.getResult() != null &&
                    tac.getResult().equals(temporal)) {

                String source = tac.getArg1();

                if (isStringLiteral(source)) {
                    return true;
                }

                Symbol sym = tacGenerator.getSymbol(source);
                if (sym != null && sym.getType() != null && sym.getType().equals("string")) {
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    /**
     * Genera impresión secuencial para "string" + int o int + "string"
     * NO imprime aquí, solo marca que este temporal representa una concatenación
     * La impresión real ocurre en print()
     */
    private void generateSequentialPrint(TACInstruction tac) {
        String result = tac.getResult();
        String arg1 = tac.getArg1();
        String arg2 = tac.getArg2();


        // NO HACER NADA MÁS
        // El temporal 'result' ahora representa una concatenación
        // Cuando llegue a print(), expandiremos todos los argumentos
    }

    /**
     * Expande y imprime un parámetro que puede ser resultado de concatenaciones
     */
    private void expandAndPrint(String param, int currentLine) {
        List<TACInstruction> instructions = tacGenerator.getInstructions();

        for (int i = currentLine - 1; i >= 0; i--) {
            TACInstruction tac = instructions.get(i);
            if (tac.getResult() != null && tac.getResult().equals(param)) {
                if (tac.getOp() == TACInstruction.OpType.BINARY_OP &&
                        tac.getOperator().equals("+") &&
                        isStringOperation(tac.getArg1(), tac.getArg2())) {

                    // Expandir recursivamente CON el contexto de línea correcto
                    expandAndPrint(tac.getArg1(), i);
                    expandAndPrint(tac.getArg2(), i);
                    return;
                }
                break;
            }
        }

        // Imprimir directamente CON el contexto de línea
        printArgument(param, currentLine);
    }

    /**
     * Imprime un argumento (puede ser string o int)
     */
    /**
     * Imprime un argumento (puede ser string o int)
     * CORREGIDO: Carga valores directamente sin depender del allocator
     */
    /**
     * Imprime un argumento (puede ser string o int)
     * @param arg El argumento a imprimir
     * @param contextLine La línea del TAC desde donde buscar el valor
     */
    private void printArgument(String arg, int contextLine) {
        // Caso 1: String literal
        if (isStringLiteral(arg)) {
            String label = stringLiterals.get(arg);
            instructions.add(MIPSInstruction.la("$a0", label));
            instructions.add(MIPSInstruction.li("$v0", 4));
            instructions.add(MIPSInstruction.syscall());
            return;
        }

        // Caso 2: Número literal
        if (isImmediate(arg)) {
            instructions.add(MIPSInstruction.li("$a0", Integer.parseInt(arg)));
            instructions.add(MIPSInstruction.li("$v0", 1));
            instructions.add(MIPSInstruction.syscall());
            return;
        }

        // Caso 3: Variable con símbolo conocido
        Symbol paramSym = tacGenerator.getSymbol(arg);
        if (paramSym != null && paramSym.getType() != null) {
            String type = paramSym.getType();

            if (type.equals("string")) {
                String stringValue = findVariableValue(arg, contextLine);
                if (stringValue != null && isStringLiteral(stringValue)) {
                    String label = stringLiterals.get(stringValue);
                    instructions.add(MIPSInstruction.la("$a0", label));
                } else {
                    String paramReg = allocator.getReg(arg);
                    instructions.add(MIPSInstruction.move("$a0", paramReg));
                }
                instructions.add(MIPSInstruction.li("$v0", 4));
                instructions.add(MIPSInstruction.syscall());
                return;
            } else if (type.equals("integer")) {
                String intValue = findVariableValue(arg, contextLine);
                if (intValue != null && isImmediate(intValue)) {
                    instructions.add(MIPSInstruction.li("$a0", Integer.parseInt(intValue)));
                } else {
                    String paramReg = allocator.getReg(arg);
                    instructions.add(MIPSInstruction.move("$a0", paramReg));
                }
                instructions.add(MIPSInstruction.li("$v0", 1));
                instructions.add(MIPSInstruction.syscall());
                return;
            }
        }

        // Caso 4: Temporal - buscar valor en contexto correcto
        if (arg.matches("^t\\d+$")) {
            String value = findVariableValue(arg, contextLine);

            if (value != null && isStringLiteral(value)) {
                String label = stringLiterals.get(value);
                instructions.add(MIPSInstruction.la("$a0", label));
                instructions.add(MIPSInstruction.li("$v0", 4));
                instructions.add(MIPSInstruction.syscall());
                return;
            }

            if (value != null && isImmediate(value)) {
                instructions.add(MIPSInstruction.li("$a0", Integer.parseInt(value)));
                instructions.add(MIPSInstruction.li("$v0", 1));
                instructions.add(MIPSInstruction.syscall());
                return;
            }

            if (value != null) {
                printArgument(value, contextLine);
                return;
            }
        }

        // Caso 5: Fallback
        String argReg = allocator.getReg(arg);
        instructions.add(MIPSInstruction.move("$a0", argReg));
        allocator.saveTemporaries();
        instructions.add(MIPSInstruction.jump(OpCode.JAL, "print"));
    }

    /**
     * Encuentra el valor asignado a una variable/temporal en el TAC
     * Busca la última asignación ANTES de maxLine
     */
    private String findVariableValue(String varName, int maxLine) {
        List<TACInstruction> tacInstructions = tacGenerator.getInstructions();

        // Buscar hacia atrás desde maxLine
        for (int i = maxLine - 1; i >= 0; i--) {
            TACInstruction tac = tacInstructions.get(i);

            // Buscar asignaciones a esta variable
            if (tac.getOp() == TACInstruction.OpType.ASSIGN &&
                    tac.getResult() != null &&
                    tac.getResult().equals(varName)) {
                return tac.getArg1();
            }
        }

        return null;
    }

    /**
     * Verifica si un temporal contiene un entero
     * Busca en las instrucciones TAC previas para ver si fue asignado desde un string
     */
    private boolean temporalContainsInt(String temporal) {
        if (temporal == null || !temporal.matches("^t\\d+$")) {
            return false;
        }

        // Buscar en las instrucciones TAC la asignación de este temporal
        for (TACInstruction tac : tacGenerator.getInstructions()) {
            if (tac.getOp() == TACInstruction.OpType.ASSIGN &&
                    tac.getResult() != null &&
                    tac.getResult().equals(temporal)) {

                String source = tac.getArg1();

                // Si se asignó desde un número literal
                if (isImmediate(source)) {
                    return true;
                }

                // Si se asignó desde una variable integer
                Symbol sym = tacGenerator.getSymbol(source);
                if (sym != null && sym.getType() != null && sym.getType().equals("integer")) {
                    return true;
                }

                // Si se asignó desde otro temporal que es int (recursivo)
                if (source != null && source.matches("^t\\d+$")) {
                    return temporalContainsInt(source);
                }
            }

            // Si fue resultado de una operación aritmética
            if (tac.getOp() == TACInstruction.OpType.BINARY_OP &&
                    tac.getResult() != null &&
                    tac.getResult().equals(temporal)) {

                String operator = tac.getOperator();

                // Operadores aritméticos producen integers
                if (operator.equals("+") || operator.equals("-") ||
                        operator.equals("*") || operator.equals("/") ||
                        operator.equals("%")) {

                    // Verificar que no sea concatenación de strings
                    // (+ puede ser tanto aritmético como concatenación)
                    if (operator.equals("+")) {
                        // Si alguno es string, es concatenación, no aritmética
                        if (isStringLiteral(tac.getArg1()) || isStringLiteral(tac.getArg2()) ||
                                isTemporalWithDirectStringAssignment(tac.getArg1()) || isTemporalWithDirectStringAssignment(tac.getArg2())) {
                            return false; // Es concatenación, no produce int
                        }
                    }

                    return true; // Es operación aritmética
                }

                // Operadores de comparación también producen integers (0 o 1)
                if (operator.equals("<") || operator.equals("<=") ||
                        operator.equals(">") || operator.equals(">=") ||
                        operator.equals("==") || operator.equals("!=")) {
                    return true;
                }
            }

            // Si fue resultado de una operación unaria
            if (tac.getOp() == TACInstruction.OpType.UNARY_OP &&
                    tac.getResult() != null &&
                    tac.getResult().equals(temporal)) {

                String operator = tac.getOperator();

                // Negación aritmética produce integer
                if (operator.equals("-")) {
                    return true;
                }

                // Negación lógica produce boolean (que es integer en MIPS)
                if (operator.equals("!")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Verifica si un valor es un string literal
     */
    private boolean isStringLiteral(String value) {
        if (value == null) return false;
        return value.startsWith("\"") && value.endsWith("\"");
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
     * Genera comparaciones: result = arg1 op arg2
     * Retorna 1 (true) o 0 (false) según la comparación
     */
    private void generateComparison(TACInstruction tac) {
        String result = tac.getResult();
        String arg1 = tac.getArg1();
        String arg2 = tac.getArg2();
        String op = tac.getOperator();

        String resultReg = allocator.getReg(result);
        String arg1Reg = allocator.getReg(arg1);

        String arg2Reg;
        boolean needsFree = false;

        // Si arg2 es inmediato, cargarlo a registro
        if (isImmediate(arg2)) {
            arg2Reg = allocator.getReg("temp_cmp_imm");
            instructions.add(MIPSInstruction.li(arg2Reg, Integer.parseInt(arg2)));
            needsFree = true;
        } else {
            arg2Reg = allocator.getReg(arg2);
        }

        // Generar comparación según el operador
        switch (op) {
            case "<":
                // result = (arg1 < arg2) ? 1 : 0
                instructions.add(MIPSInstruction.typeR(OpCode.SLT, resultReg, arg1Reg, arg2Reg));
                break;

            case "<=":
                // result = !(arg2 < arg1)
                instructions.add(MIPSInstruction.typeR(OpCode.SLT, resultReg, arg2Reg, arg1Reg));
                instructions.add(MIPSInstruction.typeI(OpCode.XORI, resultReg, resultReg, 1));
                break;

            case ">":
                // result = (arg2 < arg1)
                instructions.add(MIPSInstruction.typeR(OpCode.SLT, resultReg, arg2Reg, arg1Reg));
                break;

            case ">=":
                // result = !(arg1 < arg2)
                instructions.add(MIPSInstruction.typeR(OpCode.SLT, resultReg, arg1Reg, arg2Reg));
                instructions.add(MIPSInstruction.typeI(OpCode.XORI, resultReg, resultReg, 1));
                break;

            case "==":
                // result = (arg1 == arg2) ? 1 : 0
                String tempEq = allocator.getReg("temp_eq");
                instructions.add(MIPSInstruction.typeR(OpCode.SUB, tempEq, arg1Reg, arg2Reg));
                instructions.add(MIPSInstruction.typeR(OpCode.SEQ, resultReg, tempEq, "$zero"));
                allocator.freeRegister(tempEq);
                break;

            case "!=":
                // result = (arg1 != arg2) ? 1 : 0
                String tempNe = allocator.getReg("temp_ne");
                instructions.add(MIPSInstruction.typeR(OpCode.SUB, tempNe, arg1Reg, arg2Reg));
                instructions.add(MIPSInstruction.typeR(OpCode.SNE, resultReg, tempNe, "$zero"));
                allocator.freeRegister(tempNe);
                break;

            default:
                instructions.add(MIPSInstruction.comment("Unknown comparison: " + op));
        }

        if (needsFree) {
            allocator.freeRegister(arg2Reg);
        }

        allocator.markDirty(resultReg);
    }

    /**
     * Genera concatenación de strings: result = arg1 + arg2
     * Convierte integers a strings automáticamente
     */
    private void generateStringConcatenation(TACInstruction tac) {
        String result = tac.getResult();
        String arg1 = tac.getArg1();
        String arg2 = tac.getArg2();

        if (temporalContainsInt(arg1) || temporalContainsInt(arg2)) {
            generateSequentialPrint(tac);
            return;
        }

        instructions.add(MIPSInstruction.comment("String concat: " + result + " = " + arg1 + " + " + arg2));

        String arg1Reg = prepareStringArgument(arg1);
        String arg2Reg = prepareStringArgument(arg2);

        // Colocar argumentos en $a0 y $a1
        instructions.add(MIPSInstruction.move("$a0", arg1Reg));
        instructions.add(MIPSInstruction.move("$a1", arg2Reg));

        // Llamar función de concatenación
        allocator.saveTemporaries();
        instructions.add(MIPSInstruction.jump(OpCode.JAL, "concat_strings"));

        // El resultado está en $v0
        String resultReg = allocator.getReg(result);
        instructions.add(MIPSInstruction.move(resultReg, "$v0"));
        allocator.markDirty(resultReg);
    }

    /**
     * Prepara un argumento para concatenación
     * Convierte integers a strings si es necesario
     */
    private String prepareStringArgument(String arg) {
        // Caso 1: String literal
        if (isStringLiteral(arg)) {
            String label = stringLiterals.get(arg);
            String reg = allocator.getReg("temp_str_lit");
            instructions.add(MIPSInstruction.la(reg, label));
            return reg;
        }

        // Caso 2: Variable string normal o temporal
        return allocator.getReg(arg);
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

            expandAndPrint(param, allocator.getCurrentLine());
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
     * Genera: obj = new ClassName(params)
     * Llama constructor de superclase primero
     */
    private void generateNew(TACInstruction tac) {
        String result = tac.getResult();
        String className = tac.getArg1();
        List<String> params = tac.getParams();

        allocator.setLoadObject(true);
        instructions.add(MIPSInstruction.comment("new " + className));

        Symbol classSymbol = tacGenerator.getSymbol(className);
        if (classSymbol == null) {
            instructions.add(MIPSInstruction.comment("ERROR: Class not found: " + className));
            return;
        }

        int objectSize = calculateObjectSizeWithInheritance(classSymbol);

        // Reservar memoria
        instructions.add(MIPSInstruction.li("$a0", objectSize));
        instructions.add(MIPSInstruction.li("$v0", 9));
        instructions.add(MIPSInstruction.syscall());

        String objReg = allocator.getReg(result);
        instructions.add(MIPSInstruction.move(objReg, "$v0"));

        // Registrar manualmente en el allocator que 'result' está en $s0
        allocator.forceRegisterMapping(result, objReg);

        // Inicializar propiedades
        initializeObjectProperties(objReg, classSymbol);

        // Llamar constructor si existe
        if (hasConstructor(classSymbol)) {
            // Preparar this en $a0
            instructions.add(MIPSInstruction.move("$a0", objReg));

            // Preparar parámetros en $a1, $a2, $a3
            Register[] argRegs = {Register.A1, Register.A2, Register.A3};
            for (int i = 0; i < params.size() && i < 3; i++) {
                String paramReg = allocator.getReg(params.get(i));
                instructions.add(MIPSInstruction.move(argRegs[i].getName(), paramReg));
            }

            // Llamar constructor
            allocator.saveTemporaries();
            instructions.add(MIPSInstruction.jump(OpCode.JAL, className + "_constructor"));

            // El objeto sigue en $sx, no necesita restauración
        }
        allocator.setLoadObject(false);
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
     * Carga parámetros desde $a0-$a3
     */
    private void generateFunctionProlog(TACInstruction tac) {
        String functionName = tac.getLabel();
        currentFunction = functionName;

        Symbol funcSym = tacGenerator.getSymbol(functionName);
        boolean isMethod = funcSym != null && funcSym.getEnclosingClassName() != null;

        int paramCount = funcSym != null ? funcSym.getParamCount() : 0;
        if (isMethod) {
            functionName = funcSym.getEnclosingClassName() + "_" + functionName;
            paramCount++; // Contar 'this'
        }

        instructions.add(MIPSInstruction.label(functionName));
        int localSpace = calculateLocalSpace();

        // CORRECCIÓN: Incluir espacio para parámetros en el frame
        int paramsSpace = Math.min(paramCount, 4) * 4; // Espacio para hasta 4 params
        int frameSize = 8 + paramsSpace + localSpace; // $fp + $ra + params + locales

        // Crear stack frame
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", -frameSize));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$fp", (frameSize - 8) + "($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$ra", (frameSize - 4) + "($sp)"));
        instructions.add(MIPSInstruction.move("$fp", "$sp"));

        // Guardar parámetros DESPUÉS de $fp y $ra
        Register[] argRegs = {Register.A0, Register.A1, Register.A2, Register.A3};
        for (int i = 0; i < Math.min(paramCount, 4); i++) {
            int offset = i * 4; // Offsets 0, 4, 8, 12 desde el inicio del frame
            instructions.add(MIPSInstruction.loadStore(
                    OpCode.SW,
                    argRegs[i].getName(),
                    offset + "($fp)" // ← USAR $fp, no $sp
            ));
        }

        allocator.reset();
    }


    /**
     * Genera epílogo de función
     */
    private void generateFunctionEpilog(TACInstruction tac) {
        String functionName = tac.getLabel();
        instructions.add(MIPSInstruction.label(functionName + "_epilog"));

        Symbol funcSym = tacGenerator.getSymbol(functionName);
        boolean isMethod = funcSym != null && funcSym.getEnclosingClassName() != null;
        int paramCount = funcSym != null ? funcSym.getParamCount() : 0;
        if (isMethod) paramCount++;

        int localSpace = calculateLocalSpace();
        int paramsSpace = Math.min(paramCount, 4) * 4;
        int frameSize = 8 + paramsSpace + localSpace;

        allocator.flushAll();

        instructions.add(MIPSInstruction.move(Register.SP.getName(), Register.FP.getName()));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, Register.FP.getName(), (frameSize - 8) + "(" + Register.SP.getName() + ")"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, Register.RA.getName(), (frameSize - 4) + "(" + Register.SP.getName() + ")"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, Register.SP.getName(), Register.SP.getName(), frameSize));
        instructions.add(MIPSInstruction.jumpReg(Register.RA.getName()));

        currentFunction = null;
    }

    private void generateMethodCall(TACInstruction tac) {
        String methodAccess = tac.getArg1();
        List<String> params = tac.getParams();


        int dot = methodAccess.indexOf('.');
        String obj = methodAccess.substring(0, dot);
        String method = methodAccess.substring(dot + 1);


        instructions.add(MIPSInstruction.comment("Method call: " + methodAccess));


        String objReg = allocator.getReg(obj);
        Symbol objSym = getObjectSymbol(obj);
        if (objSym == null) {
            instructions.add(MIPSInstruction.comment("ERROR: object has no type"));
            return;
        }


        String targetClass = findClassWithMethod(objSym.getName(), method);
        if (targetClass == null) targetClass = objSym.getName();


        allocator.saveTemporaries();


        instructions.add(MIPSInstruction.move("$a0", objReg));


        Register[] argRegs = {Register.A1, Register.A2, Register.A3};
        for (int i = 0; i < Math.min(params.size(), 3); i++) {
            String r = allocator.getReg(params.get(i));
            instructions.add(MIPSInstruction.move(argRegs[i].getName(), r));
        }


        instructions.add(MIPSInstruction.jump(OpCode.JAL, targetClass + "_" + method));


        allocator.reset();
    }

    private void generateMethodCallWithReturn(TACInstruction tac) {
        String result = tac.getResult();
        String methodAccess = tac.getArg1(); // "obj.method"
        List<String> params = tac.getParams();

        int dotIndex = methodAccess.indexOf('.');
        String objName = methodAccess.substring(0, dotIndex);
        String methodName = methodAccess.substring(dotIndex + 1);

        instructions.add(MIPSInstruction.comment("Method call with return: " + result + " = " + methodAccess));

        // Obtener registro del objeto
        String objReg = allocator.getReg(objName);

        // Obtener clase del objeto
        Symbol objSymbol = tacGenerator.getSymbol(objName);
        if (objSymbol == null) {
            instructions.add(MIPSInstruction.comment("ERROR: Object not found: " + objName));
            return;
        }

        String className = objSymbol.getType();

        // Buscar metodo en la clase o superclases
        String actualClassName = findClassWithMethod(className, methodName);
        if (actualClassName == null) {
            actualClassName = className;
        }

        // Preparar parámetros: this primero ($a0), luego params
        instructions.add(MIPSInstruction.move("$a0", objReg));

        Register[] argRegs = {Register.A1, Register.A2, Register.A3};
        for (int i = 0; i < params.size() && i < 3; i++) {
            String paramReg = allocator.getReg(params.get(i));
            instructions.add(MIPSInstruction.move(argRegs[i].getName(), paramReg));
        }

        // Llamar metodo: ClassName_methodName
        allocator.saveTemporaries();
        instructions.add(MIPSInstruction.jump(OpCode.JAL, actualClassName + "_" + methodName));

        // Capturar retorno
        String resultReg = allocator.getReg(result);
        instructions.add(MIPSInstruction.move(resultReg, "$v0"));
        allocator.markDirty(resultReg);
    }

    private void initializeObjectProperties(String objReg, Symbol classSymbol) {
        if (classSymbol == null) return;

        // Primero inicializar propiedades de la superclase
        if (classSymbol.getSuperClass() != null) {
            Symbol superClass = tacGenerator.getSymbol(classSymbol.getSuperClass());
            if (superClass != null) {
                initializeObjectProperties(objReg, superClass);
            }
        }

        // Luego inicializar propiedades de esta clase
        if (classSymbol.getMembers() == null) return;

        for (Symbol member : classSymbol.getMembers().values()) {
            // Solo variables con valores por defecto
            if (member.getKind() == Symbol.Kind.VARIABLE ||
                    member.getKind() == Symbol.Kind.CONSTANT) {

                // Buscar si tiene inicialización en el TAC
                String defaultValue = findPropertyDefaultValue(classSymbol.getName(), member.getName());
                if (defaultValue != null) {
                    int offset = getPropertyOffsetWithInheritance(classSymbol, member.getName());

                    // Cargar valor por defecto
                    String tempReg = "$t9";

                    if (isStringLiteral(defaultValue)) {
                        String label = stringLiterals.get(defaultValue);
                        instructions.add(MIPSInstruction.la(tempReg, label));
                    } else if (isImmediate(defaultValue)) {
                        instructions.add(MIPSInstruction.li(tempReg, Integer.parseInt(defaultValue)));
                    } else {
                        String valueReg = allocator.getReg(defaultValue);
                        instructions.add(MIPSInstruction.move(tempReg, valueReg));
                    }

                    // Guardar en objeto
                    instructions.add(MIPSInstruction.loadStore(
                            OpCode.SW,
                            tempReg,
                            offset + "(" + objReg + ")"
                    ));
                }
            }
        }
    }

    private String findPropertyDefaultValue(String className, String propName) {
        List<TACInstruction> tacInstructions = tacGenerator.getInstructions();
        boolean inClass = false;

        for (TACInstruction tac : tacInstructions) {
            // Detectar entrada a la clase
            if (tac.getOp() == TACInstruction.OpType.LABEL_CLASS &&
                    tac.getLabel().equals(className)) {
                inClass = true;
                continue;
            }

            // Detectar salida de la clase
            if (tac.getOp() == TACInstruction.OpType.END_CLASS) {
                inClass = false;
                continue;
            }

            // Buscar asignación dentro de la clase pero fuera de funciones
            if (inClass && tac.getOp() == TACInstruction.OpType.ASSIGN) {
                if (tac.getResult() != null && tac.getResult().equals(propName)) {
                    return tac.getArg1();
                }
            }

            // No buscar dentro de funciones
            if (tac.getOp() == TACInstruction.OpType.LABEL_FUNCTION) {
                // Skip hasta END
                continue;
            }
        }

        return null;
    }

    /**
     * Busca en qué clase (actual o superclase) está definido un metodo
     */
    private String findClassWithMethod(String className, String methodName) {
        Symbol classSymbol = tacGenerator.getSymbol(className);
        if (classSymbol == null) return null;

        // Buscar en la clase actual
        if (classSymbol.getMembers() != null) {
            for (Symbol member : classSymbol.getMembers().values()) {
                if (member.getKind() == Symbol.Kind.FUNCTION &&
                        member.getName().equals(methodName)) {
                    return className;
                }
            }
        }

        // Buscar en superclase
        if (classSymbol.getSuperClass() != null) {
            return findClassWithMethod(classSymbol.getSuperClass(), methodName);
        }

        return null;
    }

    /**
     * Calcula offset de propiedad considerando herencia
     * Propiedades heredadas van primero
     */
    private int getPropertyOffsetWithInheritance(Symbol classSymbol, String propName) {
        if (classSymbol == null) return 0;

        // Construir lista ordenada de propiedades (heredadas primero)
        List<Symbol> orderedProperties = new ArrayList<>();
        collectPropertiesInOrder(classSymbol, orderedProperties);

        int offset = 0;
        for (Symbol prop : orderedProperties) {
            if (prop.getName().equals(propName)) {
                return offset;
            }
            offset += tacGenerator.typeSize(prop.getType());
        }

        return 0;
    }

    private void collectPropertiesInOrder(Symbol classSymbol, List<Symbol> result) {
        if (classSymbol == null) return;

        // Primero propiedades de la superclase
        if (classSymbol.getSuperClass() != null) {
            Symbol superClass = tacGenerator.getSymbol(classSymbol.getSuperClass());
            collectPropertiesInOrder(superClass, result);
        }

        // Luego propiedades de esta clase
        if (classSymbol.getMembers() != null) {
            for (Symbol member : classSymbol.getMembers().values()) {
                if (member.getKind() == Symbol.Kind.VARIABLE ||
                        member.getKind() == Symbol.Kind.CONSTANT) {
                    result.add(member);
                }
            }
        }
    }

    /**
     * Calcula tamaño del objeto incluyendo propiedades heredadas
     */
    private int calculateObjectSizeWithInheritance(Symbol classSymbol) {
        if (classSymbol == null) return 8;

        int totalSize = 0;

        // Primero sumar tamaño de superclase
        if (classSymbol.getSuperClass() != null) {
            Symbol superClass = tacGenerator.getSymbol(classSymbol.getSuperClass());
            if (superClass != null) {
                totalSize += calculateObjectSizeWithInheritance(superClass);
            }
        }

        // Luego sumar propiedades de esta clase
        if (classSymbol.getMembers() != null) {
            for (Symbol member : classSymbol.getMembers().values()) {
                if (member.getKind() == Symbol.Kind.VARIABLE ||
                        member.getKind() == Symbol.Kind.CONSTANT) {

                    int size = tacGenerator.typeSize(member.getType());
                    totalSize += size;
                }
            }
        }

        // Alinear a múltiplo de 4
        if (totalSize % 4 != 0) {
            totalSize += 4 - (totalSize % 4);
        }

        return totalSize > 0 ? totalSize : 8;
    }

    /**
     * Verifica si una clase tiene constructor definido
     */
    private boolean hasConstructor(Symbol classSymbol) {
        if (classSymbol == null || classSymbol.getMembers() == null) {
            return false;
        }

        for (Symbol member : classSymbol.getMembers().values()) {
            if (member.getKind() == Symbol.Kind.FUNCTION &&
                    member.getName().equals("constructor")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtiene el símbolo de un objeto (puede ser 'this' o una variable)
     */
    private Symbol getObjectSymbol(String objName) {
        if (objName.equals("this")) {
            // 'this' refiere al objeto actual, necesitamos la clase actual
            if (currentFunction != null) {
                Symbol funcSym = tacGenerator.getSymbol(currentFunction);
                if (funcSym != null && funcSym.getEnclosingClassName() != null) {
                    String className = funcSym.getEnclosingClassName();
                    return tacGenerator.getSymbol(className);
                }
            }
            return null;
        }

        // Caso 2: es variable declarada (busca símbolo)
        Symbol varSym = tacGenerator.getSymbol(objName);
        if (varSym != null) {
            return tacGenerator.getSymbol(varSym.getType());
        }

        // Caso 3: es un temporal -> debes tener un mapa tempTypeMap
        if (tempTypeMap.containsKey(objName)) {
            return tacGenerator.getSymbol(tempTypeMap.get(objName));
        }

        return null;
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

    // RUNTIME FUNCTIONS - AGREGAR AL FINAL DE LA CLASE
    /**
     * Genera todas las funciones de runtime (LLAMAR desde generate())
     */
    private void generateRuntimeFunctions() {
        generatePrintFunction();
        generateReadIntFunction();
        generatePrintBoolFunction();
        generatePrintNewlineFunction();
        generateConcatStrings();
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

    private void generateConcatStrings() {
        instructions.add(MIPSInstruction.label("concat_strings"));

        // Guardar registros
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", -20));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$ra", "16($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$s0", "12($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$s1", "8($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$s2", "4($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$s3", "0($sp)"));

        instructions.add(MIPSInstruction.move("$s0", "$a0")); // str1
        instructions.add(MIPSInstruction.move("$s1", "$a1")); // str2

        // Calcular longitud de str1
        instructions.add(MIPSInstruction.move("$t0", "$s0"));
        instructions.add(MIPSInstruction.li("$s2", 0)); // len1
        instructions.add(MIPSInstruction.label("cs_len1"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LB, "$t1", "0($t0)"));
        instructions.add(MIPSInstruction.branchUnary(OpCode.BEQZ, "$t1", "cs_len1_done"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$t0", "$t0", 1));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$s2", "$s2", 1));
        instructions.add(MIPSInstruction.jump(OpCode.J, "cs_len1"));
        instructions.add(MIPSInstruction.label("cs_len1_done"));

        // Calcular longitud de str2
        instructions.add(MIPSInstruction.move("$t0", "$s1"));
        instructions.add(MIPSInstruction.li("$s3", 0)); // len2
        instructions.add(MIPSInstruction.label("cs_len2"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LB, "$t1", "0($t0)"));
        instructions.add(MIPSInstruction.branchUnary(OpCode.BEQZ, "$t1", "cs_len2_done"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$t0", "$t0", 1));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$s3", "$s3", 1));
        instructions.add(MIPSInstruction.jump(OpCode.J, "cs_len2"));
        instructions.add(MIPSInstruction.label("cs_len2_done"));

        // Reservar memoria (len1 + len2 + 1)
        instructions.add(MIPSInstruction.typeR(OpCode.ADD, "$a0", "$s2", "$s3"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$a0", "$a0", 1));
        instructions.add(MIPSInstruction.li("$v0", 9));
        instructions.add(MIPSInstruction.syscall());
        instructions.add(MIPSInstruction.move("$t2", "$v0")); // buffer destino

        // Copiar str1
        instructions.add(MIPSInstruction.move("$t0", "$s0"));
        instructions.add(MIPSInstruction.move("$t1", "$t2"));
        instructions.add(MIPSInstruction.label("cs_copy1"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LB, "$t3", "0($t0)"));
        instructions.add(MIPSInstruction.branchUnary(OpCode.BEQZ, "$t3", "cs_copy1_done"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SB, "$t3", "0($t1)"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$t0", "$t0", 1));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$t1", "$t1", 1));
        instructions.add(MIPSInstruction.jump(OpCode.J, "cs_copy1"));
        instructions.add(MIPSInstruction.label("cs_copy1_done"));

        // Copiar str2
        instructions.add(MIPSInstruction.move("$t0", "$s1"));
        instructions.add(MIPSInstruction.label("cs_copy2"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LB, "$t3", "0($t0)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SB, "$t3", "0($t1)"));
        instructions.add(MIPSInstruction.branchUnary(OpCode.BEQZ, "$t3", "cs_done"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$t0", "$t0", 1));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$t1", "$t1", 1));
        instructions.add(MIPSInstruction.jump(OpCode.J, "cs_copy2"));

        // Retornar
        instructions.add(MIPSInstruction.label("cs_done"));
        instructions.add(MIPSInstruction.move("$v0", "$t2"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$s3", "0($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$s2", "4($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$s1", "8($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$s0", "12($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$ra", "16($sp)"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", 20));
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
        addDataVariable("concat_buffer", ".space 512");
        addDataVariable("int_buffer", ".space 32");
    }
}