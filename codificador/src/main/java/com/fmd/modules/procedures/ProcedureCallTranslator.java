package com.fmd.modules.procedures;

import java.util.*;

import com.fmd.TACGenerator;
import com.fmd.modules.Symbol;
import com.fmd.modules.TACInstruction;

/**
 * Traductor de TAC a MIPS para Llamadas y Retornos de Procedimientos
 *
 */
public class ProcedureCallTranslator {

    private List<String> mipsCode;
    private TACGenerator tacGenerator;
    private int currentStackOffset;
    private String currentFunction;

    // Constantes para el stack frame
    private static final int WORD_SIZE = 4;
    private static final int MAX_ARGS_IN_REGISTERS = 4; // $a0-$a3

    public ProcedureCallTranslator(TACGenerator generator) {
        this.mipsCode = new ArrayList<>();
        this.tacGenerator = generator;
        this.currentStackOffset = 0;
    }

    /**
     * Traduce una lista de instrucciones TAC a MIPS
     */
    public List<String> translate(List<TACInstruction> tacInstructions) {
        mipsCode.clear();

        // Header del programa MIPS
        addComment("=".repeat(60));
        addComment(" CÓDIGO MIPS GENERADO");
        addComment(" Módulo: Llamadas y Retornos de Procedimientos");
        addComment("=".repeat(60));
        addMipsInstruction("");
        addMipsInstruction(".data");
        generateDataSection(tacInstructions);
        addMipsInstruction("");
        addMipsInstruction(".text");
        addMipsInstruction(".globl main");
        addMipsInstruction("");

        // Procesar cada instrucción TAC
        for (int i = 0; i < tacInstructions.size(); i++) {
            TACInstruction tac = tacInstructions.get(i);
            translateInstruction(tac, tacInstructions, i);
        }

        // Añadir syscall exit al final (después de main)
        addMipsInstruction("");
        addComment("Exit del programa");
        addMipsInstruction("    li $v0, 10");
        addMipsInstruction("    syscall");

        return new ArrayList<>(mipsCode);
    }

    /**
     * Genera la sección .data con strings literales
     */
    private void generateDataSection(List<TACInstruction> instructions) {
        addComment("Strings literales y constantes");
        addMipsInstruction("    newline: .asciiz \"\\n\"");

        // Extraer strings del TAC
        Set<String> strings = new HashSet<>();
        int stringCounter = 0;

        for (TACInstruction instr : instructions) {
            if (instr.getOp() == TACInstruction.OpType.ASSIGN) {
                String arg = instr.getArg1();
                if (arg != null && arg.startsWith("\"") && arg.endsWith("\"")) {
                    if (!strings.contains(arg)) {
                        strings.add(arg);
                        String label = "str_" + stringCounter++;
                        addMipsInstruction("    " + label + ": .asciiz " + arg);
                    }
                }
            }
        }
    }

    /**
     * Traduce una instrucción TAC individual
     */
    private void translateInstruction(TACInstruction tac, List<TACInstruction> allInstructions, int index) {
        addComment("TAC: " + tac.toString());

        switch (tac.getOp()) {
            case LABEL:
                translateLabel(tac, allInstructions, index);
                break;
            case CALL:
            case ASSIGN_CALL:
                translateCall(tac, allInstructions, index);
                break;
            case RETURN:
                translateReturn(tac);
                break;
            case END:
                translateEnd(tac);
                break;
            case END_CLASS:
                translateEndClass(tac);
                break;
            default:
                // Otras instrucciones manejadas por otros módulos
                addComment("(Instrucción delegada a otro módulo)");
                break;
        }
    }

    // ============================================================================
    // PROLOGUE Y EPILOGUE
    // ============================================================================

    /**
     * Traduce etiquetas y genera prologue según corresponda
     */
    private void translateLabel(TACInstruction tac, List<TACInstruction> allInstructions, int index) {
        String label = tac.getLabel();

        addMipsInstruction("");
        addComment("-".repeat(50));
        addComment(" Función/Clase: " + label);
        addComment("-".repeat(50));
        addMipsInstruction(label + ":");

        // Determinar si es una función (siguiente instrucción no es otra LABEL ni END_CLASS)
        boolean isFunction = isStartOfFunction(label, allInstructions, index);

        if (isFunction) {
            currentFunction = label;

            if (label.equals("main")) {
                generateMainPrologue();
            } else {
                generatePrologue(label);
            }
        }
    }

    /**
     * Determina si una etiqueta es inicio de función
     */
    private boolean isStartOfFunction(String label, List<TACInstruction> instructions, int index) {
        // Buscar el símbolo en la tabla
        Symbol sym = tacGenerator.getSymbol(label);

        // Si es una función conocida
        if (sym != null && sym.getKind() == Symbol.Kind.FUNCTION) {
            return true;
        }

        // Si no se encuentra, asumir que NO es función si empieza con mayúscula (clase)
        if (Character.isUpperCase(label.charAt(0))) {
            return false;
        }

        // Por defecto, asumir que es función
        return true;
    }

    /**
     * Genera el prologue de una función
     *
     * Stack frame layout:
     * ----------------
     * | args > 4     |  (si hay más de 4 argumentos)
     * ----------------
     * | $ra          |  <- -4($sp) antes del ajuste
     * ----------------
     * | $fp antiguo  |  <- -8($sp) antes del ajuste
     * ----------------
     * | $fp nuevo    |  <- apunta aquí después del ajuste
     * ----------------
     * | locals       |  <- $sp apunta al final
     * ----------------
     */
    private void generatePrologue(String functionName) {
        addComment("Prologue: Preparar stack frame");

        // 1. Guardar $ra y $fp en el stack del caller
        addMipsInstruction("    sw $ra, -4($sp)     # Guardar return address");
        addMipsInstruction("    sw $fp, -8($sp)     # Guardar frame pointer");

        // 2. Ajustar $sp para hacer espacio
        addMipsInstruction("    addi $sp, $sp, -8   # Ajustar stack pointer");

        // 3. Establecer nuevo frame pointer
        addMipsInstruction("    move $fp, $sp       # Establecer nuevo frame pointer");

        // 4. Reservar espacio para variables locales
        Symbol funcSym = tacGenerator.getSymbol(functionName);
        int localSpace = 0;

        if (funcSym != null && funcSym.getLocalVarSize() > 0) {
            localSpace = funcSym.getLocalVarSize();
            // Alinear a múltiplo de 4
            if (localSpace % 4 != 0) {
                localSpace = ((localSpace / 4) + 1) * 4;
            }
            addMipsInstruction("    addi $sp, $sp, -" + localSpace +
                    "   # Reservar espacio para locales (" + localSpace + " bytes)");
        }

        addMipsInstruction("");
    }

    /**
     * Genera prologue simplificado para main
     */
    private void generateMainPrologue() {
        addComment("Prologue de main (simplificado)");
        addMipsInstruction("    move $fp, $sp       # Inicializar frame pointer");
        addMipsInstruction("");
    }

    /**
     * Genera el epilogue de una función
     */
    private void generateEpilogue(String functionName) {
        addComment("Epilogue: Limpiar stack frame y retornar");

        // 1. Restaurar $sp al frame pointer
        addMipsInstruction("    move $sp, $fp       # Restaurar stack pointer");

        // 2. Ajustar $sp para recuperar $ra y $fp
        addMipsInstruction("    addi $sp, $sp, 8    # Recuperar espacio de $ra y $fp");

        // 3. Restaurar $fp
        addMipsInstruction("    lw $fp, -8($sp)     # Restaurar frame pointer");

        // 4. Restaurar $ra
        addMipsInstruction("    lw $ra, -4($sp)     # Restaurar return address");

        // 5. Retornar al caller
        addMipsInstruction("    jr $ra              # Retornar al caller");
        addMipsInstruction("");
    }

    // ============================================================================
    // CALL y RETURN
    // ============================================================================

    /**
     * Traduce instrucción CALL o ASSIGN_CALL
     *
     * TAC: call f(t1, t2, t3)
     *      x = call f(t1, t2)
     *
     * MIPS:
     *   # Cargar parámetros en $a0-$a3 o stack
     *   lw $a0, offset_t1($fp)
     *   lw $a1, offset_t2($fp)
     *   jal f
     *   # Si hay asignación, guardar $v0
     *   sw $v0, offset_x($fp)
     */
    private void translateCall(TACInstruction tac, List<TACInstruction> allInstructions, int index) {
        String functionName = tac.getArg1();
        List<String> params = tac.getParams();

        addComment("Llamada a función: " + functionName);

        // 1. Preparar parámetros
        for (int i = 0; i < params.size(); i++) {
            String param = params.get(i);
            addComment("Parámetro " + (i + 1) + ": " + param);

            if (i < MAX_ARGS_IN_REGISTERS) {
                // Parámetros 0-3 van en $a0-$a3
                String register = "$a" + i;
                loadParameterToRegister(param, register);
            } else {
                // Parámetros 4+ van en el stack
                int stackOffset = (i - MAX_ARGS_IN_REGISTERS) * WORD_SIZE;
                addComment("Parámetro " + (i + 1) + " va en stack (offset " + stackOffset + ")");
                loadParameterToStack(param, stackOffset);
            }
        }

        // 2. Ejecutar llamada
        addMipsInstruction("    jal " + functionName);

        // 3. Si es ASSIGN_CALL, guardar resultado
        if (tac.getOp() == TACInstruction.OpType.ASSIGN_CALL) {
            String result = tac.getResult();
            addComment("Guardar resultado en " + result);
            addComment("(Almacenamiento delegado a módulo de variables)");
            // El módulo de expresiones/variables manejará esto
        }

        addMipsInstruction("");
    }

    /**
     * Carga un parámetro en un registro
     */
    private void loadParameterToRegister(String param, String register) {
        if (isNumericLiteral(param)) {
            // Es un literal numérico
            addMipsInstruction("    li " + register + ", " + param);
        } else if (isStringLiteral(param)) {
            // Es un string literal
            addComment("Cargar dirección de string");
            addMipsInstruction("    la " + register + ", str_label  # TODO: mapear " + param);
        } else {
            // Es una variable o temporal
            addComment("Cargar variable " + param + " en " + register);
            addMipsInstruction("    # TODO: lw " + register + ", offset_de_" + param + "($fp)");
        }
    }

    /**
     * Carga un parámetro en el stack
     */
    private void loadParameterToStack(String param, int offset) {
        if (isNumericLiteral(param)) {
            addMipsInstruction("    li $t0, " + param);
            addMipsInstruction("    sw $t0, " + offset + "($sp)");
        } else {
            addComment("Cargar " + param + " y guardar en stack");
            addMipsInstruction("    # TODO: lw $t0, offset_de_" + param + "($fp)");
            addMipsInstruction("    # sw $t0, " + offset + "($sp)");
        }
    }

    /**
     * Traduce instrucción RETURN
     *
     * TAC: return t1
     * MIPS:
     *   lw $v0, offset_t1($fp)
     *   j end_functionName
     */
    private void translateReturn(TACInstruction tac) {
        String returnValue = tac.getArg1();

        if (returnValue != null && !returnValue.equals("null")) {
            addComment("Retornar valor: " + returnValue);

            if (isNumericLiteral(returnValue)) {
                addMipsInstruction("    li $v0, " + returnValue);
            } else {
                addComment("Cargar " + returnValue + " en $v0");
                addMipsInstruction("    # TODO: lw $v0, offset_de_" + returnValue + "($fp)");
            }
        } else {
            addComment("Return sin valor (void)");
        }

        // Saltar al epilogue
        if (currentFunction != null && !currentFunction.equals("main")) {
            addMipsInstruction("    j end_" + currentFunction);
        }
        addMipsInstruction("");
    }

    /**
     * Traduce END (fin de función)
     *
     * TAC: end functionName
     * MIPS: Genera epilogue
     */
    private void translateEnd(TACInstruction tac) {
        String functionName = tac.getLabel();

        if (!isClassName(functionName) && !functionName.equals("main")) {
            generateEpilogue(functionName);
        } else if (functionName.equals("main")) {
            addComment("Fin de main");
        }

        addMipsInstruction("end_" + functionName + ":");
        currentFunction = null;
    }

    /**
     * Traduce END_CLASS (fin de clase)
     */
    private void translateEndClass(TACInstruction tac) {
        String className = tac.getLabel();
        addComment("Fin de clase " + className);
        addMipsInstruction("end_class_" + className + ":");
    }

    // ============================================================================
    // UTILIDADES
    // ============================================================================

    private void addMipsInstruction(String instruction) {
        mipsCode.add(instruction);
    }

    private void addComment(String comment) {
        mipsCode.add("    # " + comment);
    }

    private boolean isNumericLiteral(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isStringLiteral(String value) {
        return value != null && value.startsWith("\"") && value.endsWith("\"");
    }

    private boolean isClassName(String label) {
        // Heurística: las clases empiezan con mayúscula
        return !label.isEmpty() && Character.isUpperCase(label.charAt(0));
    }

    /**
     * Obtiene el código MIPS generado como String
     */
    public String getMipsCodeAsString() {
        StringBuilder sb = new StringBuilder();
        for (String line : mipsCode) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Imprime el código MIPS generado
     */
    public void printMipsCode() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" CÓDIGO MIPS - MÓDULO LLAMADAS Y RETORNOS");
        System.out.println("=".repeat(60));
        for (String line : mipsCode) {
            System.out.println(line);
        }
        System.out.println("=".repeat(60));
    }
}