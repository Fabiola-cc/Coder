package com.fmd;

import java.util.*;
import com.fmd.modules.TACInstruction;
import com.fmd.modules.MIPSInstruction;
import com.fmd.modules.MIPSInstruction.OpCode;
import com.fmd.modules.Register;
import com.fmd.modules.Symbol;
import jakarta.servlet.ServletOutputStream;

/**
 * Generador de código MIPS
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
                    data.append("\n");
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
    /**
     * Obtiene el tamaño total de un array desde Symbol
     * Maneja arrays 1D y 2D correctamente
     */
    private int getArraySize(Symbol sym) {
        int totalSize = sym.getSize();

        // Determinar tipo base y número de dimensiones
        String type = sym.getType();
        int dimensions = 0;
        String baseType = type;

        // Contar dimensiones ([])
        while (baseType.endsWith("[]")) {
            dimensions++;
            baseType = baseType.substring(0, baseType.length() - 2);
        }

        // Tamaño por elemento según tipo base
        int elementSize = 4; // Por defecto (integer)
        switch (baseType.toLowerCase()) {
            case "integer":
                elementSize = 4;
                break;
            case "string":
                elementSize = 8;
                break;
            case "boolean":
                elementSize = 1;
                break;
        }

        // Calcular número de elementos
        int elements = totalSize / elementSize;
        if (elements == 0) {
            elements = 10; // Default fallback
        }

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

        // Detectar "t1 = myDog.speak" ANTES del switch
        if (opType == TACInstruction.OpType.ASSIGN) {
            String dest = tac.getResult();
            String src = tac.getArg1();

            // Verificar si es método (NO propiedad)
            if (dest != null && dest.matches("^t\\d+$") &&
                    src != null && src.contains(".")) {

                int dotIndex = src.indexOf('.');
                String objName = src.substring(0, dotIndex);
                String memberName = src.substring(dotIndex + 1);

                Symbol objSymbol = getObjectSymbol(objName);
                if (objSymbol != null) {
                    String className = objSymbol.getType();
                    Symbol classSymbol = tacGenerator.getSymbol(className);

                    if (classSymbol != null && isMethod(classSymbol, memberName)) {
                        // Es método - NO generar código, solo comentar
                        instructions.add(MIPSInstruction.comment("Method reference: " + dest + " = " + src + " (resolved on call)"));
                        return; // ← SALIR AQUÍ, no entrar al switch
                    }
                }
            }
        }

        // Continuar con el procesamiento normal
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
                isMultipleCallInSequence(allocator.getCurrentLine());

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

    /**
     * Detecta si esta es la segunda llamada recursiva en una secuencia
     * Busca hacia atrás para ver si hay otra ASSIGN_CALL reciente
     */
    private void isMultipleCallInSequence(int currentLine) {
        List<TACInstruction> tacs = tacGenerator.getInstructions();

        // Buscar hacia atrás máximo 10 líneas
        int lookback = Math.max(0, currentLine - 10);
        int lookfront = Math.min(currentLine + 10, tacs.size());
        int callCount = 0;

        for (int i = currentLine - 1; i >= lookback; i--) {
            TACInstruction prevTac = tacs.get(i);

            // Si encontramos un LABEL, RETURN, o salto, ya no buscar más
            if (prevTac.getOp() == TACInstruction.OpType.LABEL ||
                    prevTac.getOp() == TACInstruction.OpType.RETURN ||
                    prevTac.getOp() == TACInstruction.OpType.GOTO)
                break;

            // Contar llamadas
            if (prevTac.getOp() == TACInstruction.OpType.ASSIGN_CALL)
                callCount++;
        }

        for (int i = currentLine; i < lookfront; i++) {
            TACInstruction prevTac = tacs.get(i);

            // Si encontramos un LABEL, RETURN, o salto, ya no buscar más
            if (prevTac.getOp() == TACInstruction.OpType.LABEL ||
                    prevTac.getOp() == TACInstruction.OpType.RETURN ||
                    prevTac.getOp() == TACInstruction.OpType.GOTO)
                break;

            // Contar llamadas
            if (prevTac.getOp() == TACInstruction.OpType.ASSIGN_CALL)
                callCount++;
        }

        // Si encontramos al menos una llamada antes, esta es la segunda
        allocator.setmultipleRecursive(callCount > 1);
    }

    private void generatePropertyGet(TACInstruction tac) {
        String dest = tac.getResult();
        String propertyAccess = tac.getArg1(); // "p.edad"

        int dotIndex = propertyAccess.indexOf('.');
        String objName = propertyAccess.substring(0, dotIndex);
        String propName = propertyAccess.substring(dotIndex + 1);

        // getObjectSymbol YA retorna el Symbol de la CLASE
        Symbol classSymbol = getObjectSymbol(objName);

        if (classSymbol != null && isMethod(classSymbol, propName)) {
            // Es método, NO generar lw
            instructions.add(MIPSInstruction.comment("Method reference: " + dest + " = " + propertyAccess + " (resolved later)"));
            return; // ← SALIR AQUÍ
        }

        // Es propiedad normal - continuar con lw
        instructions.add(MIPSInstruction.comment("Property get: " + dest + " = " + propertyAccess));

        // Obtener registro del objeto
        String objReg;
        if (objName.equals("this")) {
            objReg = "$a0";
        } else {
            // CRÍTICO: Usar el registro correcto del allocator
            objReg = allocator.getReg(objName);
        }

        if (classSymbol == null) {
            instructions.add(MIPSInstruction.comment("ERROR: Object symbol not found: " + objName));
            return;
        }

        int propOffset = getPropertyOffsetWithInheritance(classSymbol, propName);

        // Cargar propiedad
        String destReg = allocator.getReg(dest);
        instructions.add(MIPSInstruction.loadStore(
                OpCode.LW,
                destReg,
                propOffset + "(" + objReg + ")"
        ));

        allocator.markDirty(destReg);
    }

    /**
     * Verifica si un nombre es un método en la clase o superclases
     */
    private boolean isMethod(Symbol classSymbol, String memberName) {
        if (classSymbol == null || memberName == null) {
            return false;
        }

        // Buscar en los miembros de esta clase
        if (classSymbol.getMembers() != null) {
            for (Symbol member : classSymbol.getMembers().values()) {
                if (member.getName().equals(memberName)) {
                    return member.getKind() == Symbol.Kind.FUNCTION;
                }
            }
        }

        // Buscar recursivamente en la superclase
        if (classSymbol.getSuperClass() != null) {
            Symbol superClassSymbol = tacGenerator.getSymbol(classSymbol.getSuperClass());
            if (superClassSymbol != null) {
                return isMethod(superClassSymbol, memberName);
            }
        }

        return false;
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
     * Maneja string literals, evita moves redundantes, arrays
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

        // evitar mover literal a temporal
        String srcReg = allocator.getReg(src);
        String destReg = "";

        if(srcReg.startsWith("$s")){ // CASO 5.1: registros saved
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
     * Genera código MIPS para cargar dest = arr[index]
     * arrayAccess: "numbers[10]" o "matrix[i]"
     * dest: nombre de la variable destino (puede ser temporal)
     */
    /**
     * Punto de entrada para cargar arrays
     */
    private void generateArrayLoad(String dest, String arrayAccess) {
        int openBracket = arrayAccess.indexOf('[');
        int closeBracket = arrayAccess.indexOf(']');

        String arrayName = arrayAccess.substring(0, openBracket).trim();
        String index = arrayAccess.substring(openBracket + 1, closeBracket).trim();

        // CASO 1: Acceso a través de puntero (t2[1])
        if (arrayName.matches("^t\\d+$")) {
            generatePointerArrayLoad(dest, arrayName, index);
            return;
        }

        // CASO 2: Acceso a array normal
        Symbol arraySym = tacGenerator.getSymbol(arrayName);

        // Detectar si es array 2D y este acceso retornará un puntero
        boolean isRowPointer = arraySym.getType().endsWith("[][]") &&
                isUsedInSubsequentArrayAccess(dest);

        if (isRowPointer) {
            generateArrayAddressLoad(dest, arrayName, index, arraySym);
        } else {
            generateArrayValueLoad(dest, arrayName, index, arraySym);
        }
    }

    /**
     * Verifica si este temporal será usado en otro acceso
     */
    private boolean isUsedInSubsequentArrayAccess(String temp) {
        int currentLine = allocator.getCurrentLine();
        List<TACInstruction> instructions = tacGenerator.getInstructions();

        for (int i = currentLine; i < instructions.size(); i++) {
            TACInstruction tac = instructions.get(i);

            if (tac.getOp() == TACInstruction.OpType.ASSIGN &&
                    tac.getArg1() != null &&
                    tac.getArg1().startsWith(temp + "[")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Carga dirección de fila: t2 = &matrix[0]
     */
    private void generateArrayAddressLoad(String dest, String arrayName, String index, Symbol arraySym) {

        String destReg = allocator.getReg(dest);
        int cols = getArray2DColumns(arraySym);

        if (isImmediate(index)) {
            int rowIdx = Integer.parseInt(index);
            int rowOffset = rowIdx * cols * 4;

            if (arraySym.isLocal()) {
                int totalOffset = arraySym.getOffset() + rowOffset;
                instructions.add(MIPSInstruction.typeI(
                        MIPSInstruction.OpCode.ADDI,
                        destReg,
                        "$fp",
                        totalOffset
                ));
            } else {
                instructions.add(MIPSInstruction.la(destReg, arrayName));
                if (rowOffset > 0) {
                    instructions.add(MIPSInstruction.typeI(
                            MIPSInstruction.OpCode.ADDI,
                            destReg,
                            destReg,
                            rowOffset
                    ));
                }
            }
        } else {
            String indexReg = allocator.getReg(index);
            String tempReg = "$t8";

            instructions.add(MIPSInstruction.li(tempReg, cols));
            instructions.add(MIPSInstruction.typeR(
                    MIPSInstruction.OpCode.MUL,
                    tempReg,
                    indexReg,
                    tempReg
            ));

            instructions.add(MIPSInstruction.typeR(
                    MIPSInstruction.OpCode.SLL,
                    tempReg,
                    tempReg,
                    "2"
            ));

            if (arraySym.isLocal()) {
                int baseOffset = arraySym.getOffset();
                instructions.add(MIPSInstruction.typeI(
                        MIPSInstruction.OpCode.ADDI,
                        destReg,
                        "$fp",
                        baseOffset
                ));
            } else {
                instructions.add(MIPSInstruction.la(destReg, arrayName));
            }

            instructions.add(MIPSInstruction.typeR(
                    MIPSInstruction.OpCode.ADD,
                    destReg,
                    destReg,
                    tempReg
            ));
        }

        allocator.ensureBinding(dest, destReg);
    }

    /**
     * Carga valor 1D: t1 = numbers[0]
     */
    private void generateArrayValueLoad(String dest, String arrayName, String index, Symbol arraySym) {

        String destReg = allocator.getReg(dest);

        if (isImmediate(index)) {
            int idx = Integer.parseInt(index);
            int offset = idx * 4;

            if (arraySym.isLocal()) {
                int totalOffset = arraySym.getOffset() + offset;
                instructions.add(MIPSInstruction.loadStore(
                        MIPSInstruction.OpCode.LW,
                        destReg,
                        totalOffset + "($fp)"
                ));
            } else {
                instructions.add(MIPSInstruction.loadStore(
                        MIPSInstruction.OpCode.LW,
                        destReg,
                        arrayName + "+" + offset
                ));
            }
        } else {
            String indexReg = allocator.getReg(index);
            String addrReg = "$t8";
            String baseReg = "$t9";

            instructions.add(MIPSInstruction.typeR(
                    MIPSInstruction.OpCode.SLL,
                    addrReg,
                    indexReg,
                    "2"
            ));

            if (arraySym.isLocal()) {
                int baseOffset = arraySym.getOffset();
                instructions.add(MIPSInstruction.typeI(
                        MIPSInstruction.OpCode.ADDI,
                        baseReg,
                        "$fp",
                        baseOffset
                ));
            } else {
                instructions.add(MIPSInstruction.la(baseReg, arrayName));
            }

            instructions.add(MIPSInstruction.typeR(
                    MIPSInstruction.OpCode.ADD,
                    addrReg,
                    baseReg,
                    addrReg
            ));

            instructions.add(MIPSInstruction.loadStore(
                    MIPSInstruction.OpCode.LW,
                    destReg,
                    "0(" + addrReg + ")"
            ));
        }

        allocator.ensureBinding(dest, destReg);
    }

    /**
     * Carga desde puntero: t3 = t2[1]
     */
    private void generatePointerArrayLoad(String dest, String rowPointer, String index) {

        String destReg = allocator.getReg(dest);
        String pointerReg = allocator.getReg(rowPointer);

        if (isImmediate(index)) {
            int idx = Integer.parseInt(index);
            int offset = idx * 4;

            instructions.add(MIPSInstruction.loadStore(
                    MIPSInstruction.OpCode.LW,
                    destReg,
                    offset + "(" + pointerReg + ")"
            ));
        } else {
            String indexReg = allocator.getReg(index);
            String tempReg = "$t8";

            instructions.add(MIPSInstruction.typeR(
                    MIPSInstruction.OpCode.SLL,
                    tempReg,
                    indexReg,
                    "2"
            ));

            instructions.add(MIPSInstruction.typeR(
                    MIPSInstruction.OpCode.ADD,
                    tempReg,
                    pointerReg,
                    tempReg
            ));

            instructions.add(MIPSInstruction.loadStore(
                    MIPSInstruction.OpCode.LW,
                    destReg,
                    "0(" + tempReg + ")"
            ));
        }

        allocator.ensureBinding(dest, destReg);
    }


    /**
     * Almacena en array: arr[idx] = src
     */
    private void generateArrayStore(String arrayAccess, String src) {

        if (arrayAccess.contains("][")) {
            generateArray2DStore(arrayAccess, src);
            return;
        }

        int openBracket = arrayAccess.indexOf('[');
        int closeBracket = arrayAccess.indexOf(']');

        if (openBracket < 0 || closeBracket < 0) {
            return;
        }

        String arrayName = arrayAccess.substring(0, openBracket).trim();
        String index = arrayAccess.substring(openBracket + 1, closeBracket).trim();

        Symbol arraySym = tacGenerator.getSymbol(arrayName);

        // Obtener registro con el valor a guardar
        String srcReg;
        if (isImmediate(src)) {
            srcReg = allocator.getReg("__arr_src_tmp");
            instructions.add(MIPSInstruction.li(srcReg, Integer.parseInt(src)));
        } else {
            srcReg = allocator.getReg(src);
        }

        // CASO 1: ÍNDICE INMEDIATO
        if (isImmediate(index)) {
            int idx = Integer.parseInt(index);
            int elementOffset = idx * 4;

            if (arraySym.isLocal()) {
                // LOCAL: guardar en $fp + baseOffset + elementOffset
                int baseOffset = arraySym.getOffset();
                int totalOffset = baseOffset + elementOffset;

                instructions.add(MIPSInstruction.loadStore(
                        MIPSInstruction.OpCode.SW,
                        srcReg,
                        totalOffset + "($fp)"
                ));
            } else {
                // GLOBAL: usar label
                instructions.add(MIPSInstruction.loadStore(
                        MIPSInstruction.OpCode.SW,
                        srcReg,
                        arrayName + "+" + elementOffset
                ));
            }

            if (isImmediate(src)) {
                allocator.freeRegister(srcReg);
            }
            return;
        }

        //  CASO 2: ÍNDICE VARIABLE
        String indexReg = allocator.getReg(index);
        String addrReg = allocator.getReg("__arr_addr");
        String baseReg = allocator.getReg("__arr_base");

        // offset = index * 4
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.SLL,
                addrReg,
                indexReg,
                "2"
        ));

        if (arraySym.isLocal()) {
            // LOCAL: base = $fp + offset_base
            int baseOffset = arraySym.getOffset();
            instructions.add(MIPSInstruction.typeI(
                    MIPSInstruction.OpCode.ADDI,
                    baseReg,
                    "$fp",
                    baseOffset
            ));
        } else {
            // GLOBAL: cargar dirección
            instructions.add(MIPSInstruction.la(baseReg, arrayName));
        }

        // Dirección efectiva
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.ADD,
                addrReg,
                baseReg,
                addrReg
        ));

        // Guardar valor
        instructions.add(MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.SW,
                srcReg,
                "0(" + addrReg + ")"
        ));

        // Liberar registros temporales
        allocator.freeRegister(indexReg);
        allocator.freeRegister(addrReg);
        allocator.freeRegister(baseReg);

        if (isImmediate(src)) {
            allocator.freeRegister(srcReg);
        }
    }

    /**
     * Obtiene el número de columnas de un array 2D escaneando el TAC
     * Busca todos los accesos a matrix[i][j] y determina el máximo j
     */
    private int getArray2DColumns(Symbol arraySym) {
        String arrayName = arraySym.getName();
        int maxCol = 0;
        int maxRow = 0;

        List<TACInstruction> instructions = tacGenerator.getInstructions();

        for (TACInstruction tac : instructions) {
            // Buscar en result (para stores: matrix[i][j] = value)
            if (tac.getResult() != null && tac.getResult().contains(arrayName + "[")) {
                int[] dims = extract2DIndices(tac.getResult(), arrayName);
                if (dims != null) {
                    maxRow = Math.max(maxRow, dims[0]);
                    maxCol = Math.max(maxCol, dims[1]);
                }
            }

            // Buscar en arg1 (para loads: var = matrix[i][j])
            if (tac.getArg1() != null && tac.getArg1().contains(arrayName + "[")) {
                int[] dims = extract2DIndices(tac.getArg1(), arrayName);
                if (dims != null) {
                    maxRow = Math.max(maxRow, dims[0]);
                    maxCol = Math.max(maxCol, dims[1]);
                }
            }
        }

        // Número de columnas = maxCol + 1 (porque indices empiezan en 0)
        int cols = maxCol + 1;

        // Fallback si no encontramos accesos
        if (cols == 0) {
            System.err.println("WARNING: No se pudieron inferir dimensiones de " + arrayName + ", usando 2 por defecto");
            return 2;
        }

        return cols;
    }

    /**
     * Extrae los índices [i][j] de un string como "matrix[0][1]"
     * @return int[] {row, col} o null si no es un acceso 2D válido
     */
    private int[] extract2DIndices(String arrayAccess, String arrayName) {
        if (!arrayAccess.startsWith(arrayName + "[")) {
            return null;
        }

        // Parsear: matrix[0][1]
        int firstOpen = arrayAccess.indexOf('[');
        int firstClose = arrayAccess.indexOf(']');
        int secondOpen = arrayAccess.indexOf('[', firstClose);
        int secondClose = arrayAccess.indexOf(']', secondOpen);

        if (firstOpen < 0 || secondOpen < 0) {
            return null; // No es 2D
        }

        try {
            String rowStr = arrayAccess.substring(firstOpen + 1, firstClose).trim();
            String colStr = arrayAccess.substring(secondOpen + 1, secondClose).trim();

            // Solo procesar si ambos son constantes
            if (!isImmediate(rowStr) || !isImmediate(colStr)) {
                return null; // Índices variables, no podemos inferir
            }

            int row = Integer.parseInt(rowStr);
            int col = Integer.parseInt(colStr);

            return new int[]{row, col};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Maneja store en array 2D: matrix[i][j] = value
     */
    private void generateArray2DStore(String arrayAccess, String src) {
        // Parsear: matrix[0][1]
        int firstOpen = arrayAccess.indexOf('[');
        int firstClose = arrayAccess.indexOf(']');
        int secondOpen = arrayAccess.indexOf('[', firstClose);
        int secondClose = arrayAccess.indexOf(']', secondOpen);

        String arrayName = arrayAccess.substring(0, firstOpen).trim();
        String row = arrayAccess.substring(firstOpen + 1, firstClose).trim();
        String col = arrayAccess.substring(secondOpen + 1, secondClose).trim();

        Symbol arraySym = tacGenerator.getSymbol(arrayName);

        // Obtener dimensiones del array
        int cols = getArray2DColumns(arraySym);  // Necesitas obtener número de columnas

        // Cargar valor
        String srcReg;
        if (isImmediate(src)) {
            srcReg = "$t0";
            instructions.add(MIPSInstruction.li(srcReg, Integer.parseInt(src)));
        } else {
            srcReg = allocator.getReg(src);
        }

        // CASO 1: Ambos índices son constantes
        if (isImmediate(row) && isImmediate(col)) {
            int rowIdx = Integer.parseInt(row);
            int colIdx = Integer.parseInt(col);
            int offset = (rowIdx * cols + colIdx) * 4;

            if (arraySym.isLocal()) {
                int totalOffset = arraySym.getOffset() + offset;
                instructions.add(MIPSInstruction.loadStore(
                        MIPSInstruction.OpCode.SW,
                        srcReg,
                        totalOffset + "($fp)"
                ));
            } else {
                instructions.add(MIPSInstruction.loadStore(
                        MIPSInstruction.OpCode.SW,
                        srcReg,
                        arrayName + "+" + offset
                ));
            }
            return;
        }

        // CASO 2: Índices variables
        // offset = (row * cols + col) * 4
        String rowReg = allocator.getReg(row);
        String colReg = allocator.getReg(col);

        String tempReg = "$t8";
        String baseReg = "$t9";

        // temp = row * cols
        instructions.add(MIPSInstruction.li(tempReg, cols));
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.MUL,
                tempReg,
                rowReg,
                tempReg
        ));

        // temp = temp + col
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.ADD,
                tempReg,
                tempReg,
                colReg
        ));

        // temp = temp * 4 (shift left 2)
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.SLL,
                tempReg,
                tempReg,
                "2"
        ));

        // Cargar base del array
        if (arraySym.isLocal()) {
            int baseOffset = arraySym.getOffset();
            instructions.add(MIPSInstruction.typeI(
                    MIPSInstruction.OpCode.ADDI,
                    baseReg,
                    "$fp",
                    baseOffset
            ));
        } else {
            instructions.add(MIPSInstruction.la(baseReg, arrayName));
        }

        // Dirección efectiva
        instructions.add(MIPSInstruction.typeR(
                MIPSInstruction.OpCode.ADD,
                tempReg,
                baseReg,
                tempReg
        ));

        // Guardar valor
        instructions.add(MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.SW,
                srcReg,
                "0(" + tempReg + ")"
        ));
    }

    /**
     * DISPATCHER PRINCIPAL para operaciones binarias
     * Detecta el tipo de operación y delega al generador apropiado
     *
     * Casos:
     * 1. Operadores relacionales (<, >, ==, etc.) → Comparación
     * 2. Operadores aritméticos con strings (+) → Concatenación
     * 3. Operadores aritméticos normales (+, -, *, /) → Aritmética
     * 4. Operadores lógicos (&&, ||)
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

        // Caso 3: Rastrear temporales que vienen de property access a strings
        if (isTemporalWithStringPropertyAccess(arg1) || isTemporalWithStringPropertyAccess(arg2)) {
            return true;
        }

        // Caso 4: Temporal con asignación directa de string
        if (isTemporalWithDirectStringAssignment(arg1) || isTemporalWithDirectStringAssignment(arg2)) {
            return true;
        }

        return false;
    }

    /**
     * Verifica si un temporal viene de un property access a string
     * Ejemplo: t1 = this.name, donde name es string
     */
    private boolean isTemporalWithStringPropertyAccess(String temporal) {
        if (temporal == null || !temporal.matches("^t\\d+$")) {
            return false;
        }

        // Buscar la asignación de este temporal
        for (int i = tacGenerator.getInstructions().size() - 1; i >= 0; i--) {
            TACInstruction tac = tacGenerator.getInstructions().get(i);

            if (tac.getOp() == TACInstruction.OpType.ASSIGN &&
                    tac.getResult() != null &&
                    tac.getResult().equals(temporal)) {

                String source = tac.getArg1();

                // Verificar si es property access (obj.prop o this.prop)
                if (source != null && source.contains(".")) {
                    int dotIndex = source.indexOf('.');
                    String objName = source.substring(0, dotIndex);
                    String propName = source.substring(dotIndex + 1);

                    // Obtener tipo de la propiedad
                    if (objName.equals("this")) {
                        // Buscar en la clase actual
                        if (currentFunction != null) {
                            Symbol funcSym = tacGenerator.getSymbol(currentFunction);
                            if (funcSym != null && funcSym.getEnclosingClassName() != null) {
                                String className = funcSym.getEnclosingClassName();
                                Symbol classSym = tacGenerator.getSymbol(className);

                                if (classSym != null && classSym.getMembers() != null) {
                                    Symbol propSym = classSym.getMembers().get(propName);
                                    if (propSym != null && propSym.getType() != null) {
                                        return propSym.getType().equals("string");
                                    }
                                }
                            }
                        }
                    } else {
                        // Objeto normal
                        Symbol classSymbol = getObjectSymbol(objName);
                        if (classSymbol != null && classSymbol.getMembers() != null) {
                            Symbol propSym = classSymbol.getMembers().get(propName);
                            if (propSym != null && propSym.getType() != null) {
                                return propSym.getType().equals("string");
                            }
                        }
                    }
                }

                break;
            }
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

                //  SOLO considerar string si es LITERAL directo
                if (isStringLiteral(source)) {
                    return true;
                }

                Symbol sym = tacGenerator.getSymbol(source);
                if (sym != null && sym.getType() != null && sym.getType().equals("string")) {
                    return true;
                }

                // Si no es ninguna de las anteriores, NO es string
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

            // Obtener el registro donde está el temporal
            String argReg = allocator.getReg(arg);

            // Determinar el tipo para saber qué syscall usar
            if (temporalContainsInt(arg)) {
                instructions.add(MIPSInstruction.move("$a0", argReg));
                instructions.add(MIPSInstruction.li("$v0", 1));
                instructions.add(MIPSInstruction.syscall());
            } else if (isTemporalWithDirectStringAssignment(arg)) {
                instructions.add(MIPSInstruction.move("$a0", argReg));
                instructions.add(MIPSInstruction.li("$v0", 4));
                instructions.add(MIPSInstruction.syscall());
            } else {
                // Fallback genérico
                instructions.add(MIPSInstruction.move("$a0", argReg));
                allocator.saveTemporaries();
                instructions.add(MIPSInstruction.jump(OpCode.JAL, "print"));
            }
            return;
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
     * Verifica si un temporal contiene un string
     * VERSIÓN CORREGIDA: Evita recursión infinita con Set de visitados
     */
    private boolean temporalContainsString(String temporal) {
        return temporalContainsStringHelper(temporal, new HashSet<>());
    }

    private boolean temporalContainsStringHelper(String temporal, Set<String> visited) {
        if (temporal == null || !temporal.matches("^t\\d+$")) {
            return false;
        }

        // Evitar ciclos infinitos
        if (visited.contains(temporal)) {
            return false;
        }
        visited.add(temporal);

        // Buscar en las instrucciones TAC la asignación de este temporal
        for (TACInstruction tac : tacGenerator.getInstructions()) {
            if (tac.getOp() == TACInstruction.OpType.ASSIGN &&
                    tac.getResult() != null &&
                    tac.getResult().equals(temporal)) {

                String source = tac.getArg1();

                // Si se asignó desde un string literal
                if (isStringLiteral(source)) {
                    return true;
                }

                // Si se asignó desde una variable string
                Symbol sym = tacGenerator.getSymbol(source);
                if (sym != null && sym.getType() != null && sym.getType().equals("string")) {
                    return true;
                }

                // Si se asignó desde otro temporal que es string (recursivo CON visited)
                if (source != null && source.matches("^t\\d+$")) {
                    return temporalContainsStringHelper(source, visited);
                }
            }

            // Si fue resultado de una concatenación previa
            if (tac.getOp() == TACInstruction.OpType.BINARY_OP &&
                    tac.getResult() != null &&
                    tac.getResult().equals(temporal) &&
                    tac.getOperator().equals("+")) {

                // Si cualquiera de sus operandos era string, el resultado es string
                if (isStringLiteral(tac.getArg1()) || isStringLiteral(tac.getArg2())) {
                    return true;
                }

                // Recursión con visited
                if (temporalContainsStringHelper(tac.getArg1(), visited) ||
                        temporalContainsStringHelper(tac.getArg2(), visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Verifica si un temporal contiene un entero
     * Busca en las instrucciones TAC previas para ver si fue asignado desde un string
     */
    private boolean temporalContainsInt(String temporal) {
        return temporalContainsIntHelper(temporal, new HashSet<>());
    }

    private boolean temporalContainsIntHelper(String temporal, Set<String> visited) {
        if (temporal == null || !temporal.matches("^t\\d+$")) {
            return false;
        }

        // Evitar ciclos
        if (visited.contains(temporal)) {
            return false;
        }
        visited.add(temporal);

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
                    return temporalContainsIntHelper(source, visited);
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
                                temporalContainsStringHelper(tac.getArg1(), new HashSet<>()) ||
                                temporalContainsStringHelper(tac.getArg2(), new HashSet<>())) {
                            return false;
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

                if (operator.equals("-") || operator.equals("!")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Resuelve una llamada indirecta a método (cuando el TAC es "call t1()")
     * Busca en TAC previo si t1 fue asignado como "obj.method"
     * Retorna [objName, methodName, className] o null
     */
    private String[] resolveIndirectMethodCall(String tempVar) {

        if (tempVar == null || !tempVar.matches("^t\\d+$")) {
            return null;
        }

        List<TACInstruction> instructions = tacGenerator.getInstructions();

        // Buscar hacia atrás la asignación: t1 = myDog.speak
        for (int i = instructions.size() - 1; i >= 0; i--) {
            TACInstruction tac = instructions.get(i);

            if (tac.getOp() == TACInstruction.OpType.ASSIGN &&
                    tac.getResult() != null &&
                    tac.getResult().equals(tempVar)) {

                String source = tac.getArg1();

                if (source != null && source.contains(".")) {
                    int dotIndex = source.indexOf('.');
                    String objName = source.substring(0, dotIndex);
                    String methodName = source.substring(dotIndex + 1);

                    Symbol objSymbol = getObjectSymbol(objName);
                    if (objSymbol == null) {
                        return null;
                    }

                    String className = objSymbol.getName();  // ← Usar getName() porque ya es el Symbol de la clase
                    if (className == null) {
                        return null;
                    }

                    // Buscar clase real del método (puede estar en superclase)
                    String actualClassName = findClassWithMethod(className, methodName);
                    if (actualClassName == null) {
                        actualClassName = className;
                    }

                    return new String[]{objName, methodName, actualClassName};
                }

                break;
            }
        }

        return null;
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
            arg2Reg = allocator.getReg("temp_cmp_imm_" + result); // Nombre único
            instructions.add(MIPSInstruction.li(arg2Reg, Integer.parseInt(arg2)));
            needsFree = true;
        } else {
            arg2Reg = allocator.getReg(arg2);
        }

        // Generar comparación según el operador
        switch (op) {
            case "<=":
                // temp = (arg1 <= arg2) = !(arg1 > arg2) = !(arg2 < arg1)
                String tempLe = allocator.getReg("temp_le_" + result); // Nombre único
                instructions.add(MIPSInstruction.typeR(OpCode.SLT, tempLe, arg2Reg, arg1Reg));
                instructions.add(MIPSInstruction.typeI(OpCode.XORI, resultReg, tempLe, 1));
                allocator.freeRegister(tempLe);
                break;

            case "<":
                instructions.add(MIPSInstruction.typeR(OpCode.SLT, resultReg, arg1Reg, arg2Reg));
                break;

            case ">":
                instructions.add(MIPSInstruction.typeR(OpCode.SLT, resultReg, arg2Reg, arg1Reg));
                break;

            case ">=":
                String tempGe = allocator.getReg("temp_ge_" + result);
                instructions.add(MIPSInstruction.typeR(OpCode.SLT, tempGe, arg1Reg, arg2Reg));
                instructions.add(MIPSInstruction.typeI(OpCode.XORI, resultReg, tempGe, 1));
                allocator.freeRegister(tempGe);
                break;

            case "==":
                String tempEq = allocator.getReg("temp_eq_" + result);
                instructions.add(MIPSInstruction.typeR(OpCode.SUB, tempEq, arg1Reg, arg2Reg));
                instructions.add(MIPSInstruction.typeR(OpCode.SEQ, resultReg, tempEq, "$zero"));
                allocator.freeRegister(tempEq);
                break;

            case "!=":
                String tempNe = allocator.getReg("temp_ne_" + result);
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

        // CASO 1: Comparación directa - if x < y goto L1
        if (relop != null && !relop.isEmpty()) {
            String arg1Reg = allocator.getReg(arg1);
            String arg2Reg;
            boolean needsFree = false;

            if (isImmediate(arg2)) {
                arg2Reg = allocator.getReg("temp_if_cmp");
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
            return;
        }

        // CASO 2: Variable booleana - if t2 == 0 goto L1
        // Esto significa: "Si la condición es FALSA, salta"
        String condReg = allocator.getReg(arg1);

        if (arg2 != null && arg2.equals("0")) {
            // if condition == 0 goto label → beqz condition, label
            instructions.add(MIPSInstruction.branchUnary(OpCode.BEQZ, condReg, label));
        } else if (arg2 != null && arg2.equals("1")) {
            // if condition == 1 goto label → bnez condition, label
            instructions.add(MIPSInstruction.branchUnary(OpCode.BNEZ, condReg, label));
        } else if (arg2 != null) {
            // if condition == X goto label → beq condition, X, label
            String arg2Reg;
            boolean needsFree = false;

            if (isImmediate(arg2)) {
                arg2Reg = allocator.getReg("temp_if_bool");
                instructions.add(MIPSInstruction.li(arg2Reg, Integer.parseInt(arg2)));
                needsFree = true;
            } else {
                arg2Reg = allocator.getReg(arg2);
            }

            instructions.add(MIPSInstruction.branch(OpCode.BEQ, condReg, arg2Reg, label));

            if (needsFree) {
                allocator.freeRegister(arg2Reg);
            }
        } else {
            // if condition goto label (sin comparación explícita)
            // Asume: salta si condition != 0
            instructions.add(MIPSInstruction.branchUnary(OpCode.BNEZ, condReg, label));
        }
    }

    /**
     * Genera llamada a función sin asignación
     * Guarda contexto antes de llamadas (incluye recursión)
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

        // CRÍTICO PARA RECURSIÓN: Guardar contexto ANTES de pasar parámetros
        // Esto incluye variables locales que pueden ser usadas después del return
        allocator.saveTemporaries();

        // Pasar parámetros
        generateParameters(params);

        // Llamar función (puede ser recursiva)
        instructions.add(MIPSInstruction.jump(OpCode.JAL, functionName));

        // Después de retornar, el contexto se restaura automáticamente
        // porque cada llamada tiene su propio frame
    }

    /**
     * Genera llamada a función con asignación
     */
    private void generateAssignCall(TACInstruction tac) {
        String result = tac.getResult();
        String functionName = tac.getArg1();
        List<String> params = tac.getParams();

        // Detectar si es llamada indirecta a método heredado
        String[] methodInfo = resolveIndirectMethodCall(functionName);

        if (methodInfo != null) {
            String objName = methodInfo[0];
            String methodName = methodInfo[1];
            String className = methodInfo[2];

            instructions.add(MIPSInstruction.comment("Indirect method call: " + result + " = " + objName + "." + methodName + "()"));

            // Obtener registro del objeto
            String objReg = allocator.getReg(objName);
            instructions.add(MIPSInstruction.move("$a0", objReg));

            // Parámetros adicionales
            Register[] argRegs = {Register.A1, Register.A2, Register.A3};
            for (int i = 0; i < params.size() && i < 3; i++) {
                String paramReg = allocator.getReg(params.get(i));
                instructions.add(MIPSInstruction.move(argRegs[i].getName(), paramReg));
            }

            // Llamar: ClassName_methodName
            allocator.saveTemporaries();
            instructions.add(MIPSInstruction.jump(OpCode.JAL, className + "_" + methodName));

            // Capturar retorno
            String resultReg = allocator.getReg(result);
            instructions.add(MIPSInstruction.move(resultReg, "$v0"));
            allocator.markDirty(resultReg);

            return;
        }

        // Caso normal (función directa)
        generateParameters(params);
        allocator.saveTemporaries();
        instructions.add(MIPSInstruction.jump(OpCode.JAL, functionName));

        String resultReg = allocator.getReg(result);
        instructions.add(MIPSInstruction.move(resultReg, Register.V0.getName()));
        allocator.markDirty(resultReg);

        allocator.setmultipleRecursive(false);
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
        int paramsSpace = Math.min(paramCount, 4) * 4;

        // Reservar espacio para TODOS los registros $s que se usarán
        int savedRegsSpace = 32; // 8 registros $s × 4 bytes = 32 bytes

        // Frame layout
        int frameSize = 8 + savedRegsSpace + paramsSpace + localSpace;

        // 1. Reservar espacio en el stack
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", -frameSize));

        // 2. Guardar $fp ANTERIOR
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$fp", "0($sp)"));

        // 3. Guardar $ra
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$ra", "4($sp)"));

        // 4. **NUEVO: Guardar TODOS los registros $s0-$s7**
        for (int i = 0; i < allocator.getSavedRegisters().length; i++) {
            int offset = 8 + (i * 4);
            instructions.add(MIPSInstruction.loadStore(
                    OpCode.SW,
                    allocator.getSavedRegisters()[i],
                    offset + "($sp)"
            ));
        }
        instructions.add(MIPSInstruction.comment("Saved $s0-$s7"));

        // 5. Establecer nuevo $fp
        instructions.add(MIPSInstruction.move("$fp", "$sp"));

        // Guardar parámetros DESPUÉS de $fp y $ra
        Register[] argRegs = {Register.A0, Register.A1, Register.A2, Register.A3};
        for (int i = 0; i < Math.min(paramCount, 4); i++) {
            int offset = 8 + savedRegsSpace + (i * 4); // 8 + 32 = 40
            instructions.add(MIPSInstruction.loadStore(
                    OpCode.SW,
                    argRegs[i].getName(),
                    offset + "($fp)"
            ));
            instructions.add(MIPSInstruction.comment("Saved param " + i + " at " + offset + "($fp)"));
        }

        instructions.add(MIPSInstruction.comment("Frame size: " + frameSize +
                " (8 overhead + 32 saved regs + " + paramsSpace +
                " params + " + localSpace + " locals)"));

        // 7. Reset allocator y cargar parámetros
        allocator.reset();

        if (funcSym != null) {
            loadFunctionParameters(funcSym, isMethod, savedRegsSpace);
        }
    }

    /**
     * Carga los parámetros desde el stack frame
     * Considera el espacio de registros saved
     */
    private void loadFunctionParameters(Symbol funcSym, boolean isMethod, int savedRegsSpace) {
        if (funcSym == null || funcSym.getParams() == null) return;

        List<Symbol> params = funcSym.getParams();
        instructions.add(MIPSInstruction.comment("Loading parameters from frame"));

        for (int i = 0; i < params.size(); i++) {
            Symbol param = params.get(i);
            String paramName = param.getName();

            // OFFSET CORREGIDO: 8 (overhead) + 32 (saved regs) + param_index * 4
            int paramOffset = 8 + savedRegsSpace + (i * 4); // = 40 + i*4

            String paramReg;
            if (i < 4) {
                paramReg = "$s" + i;
                allocator.forceRegisterMapping(paramName, paramReg);
            } else {
                paramReg = allocator.getReg(paramName);
            }

            instructions.add(MIPSInstruction.loadStore(
                    OpCode.LW,
                    paramReg,
                    paramOffset + "($fp)"
            ));

            instructions.add(MIPSInstruction.comment(
                    "Loaded " + paramName + " from " + paramOffset + "($fp) into " + paramReg
            ));

            allocator.markClean(paramReg);
        }
    }

    /**
     * Genera epílogo de función - CON RESTAURACIÓN DE REGISTROS $s
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
        int savedRegsSpace = 32; // 8 registros $s
        int frameSize = 8 + savedRegsSpace + paramsSpace + localSpace;

        // PASO 1: Guardar registros dirty
        allocator.flushAll();

        // PASO 2: **RESTAURAR registros $s0-$s7**
        for (int i = 0; i < allocator.getSavedRegisters().length; i++) {
            int offset = 8 + (i * 4);
            instructions.add(MIPSInstruction.loadStore(
                    OpCode.LW,
                    allocator.getSavedRegisters()[i],
                    offset + "($fp)"
            ));
        }
        instructions.add(MIPSInstruction.comment("Restored $s0-$s7"));

        // PASO 3: Restaurar $fp anterior
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$t9", "0($fp)"));

        // PASO 4: Restaurar $ra
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$ra", "4($fp)"));

        // PASO 5: Restaurar $sp
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$fp", frameSize));

        // PASO 6: Restaurar $fp
        instructions.add(MIPSInstruction.move("$fp", "$t9"));

        // PASO 7: Retornar
        instructions.add(MIPSInstruction.jumpReg("$ra"));

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
        allocator.setmultipleRecursive(false);
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
            String typeName = varSym.getType();
            if (typeName != null) {
                Symbol classSymbol = tacGenerator.getSymbol(typeName);
                return classSymbol;
            } else {
            }
        } else {
        }

        // Caso 3: es un temporal
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