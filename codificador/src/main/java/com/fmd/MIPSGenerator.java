package com.fmd;

import java.util.*;
import com.fmd.modules.TACInstruction;
import com.fmd.modules.MIPSInstruction;
import com.fmd.modules.MIPSInstruction.OpCode;

/**
 * Generador de código MIPS a partir de código intermedio (Three-Address Code)
 */
public class MIPSGenerator {
    private RegisterAllocator allocator;
    private List<MIPSInstruction> instructions;
    private Map<String, String> dataSegment;
    private int labelCounter;
    private String currentFunction;

    public MIPSGenerator(TACGenerator tacGenerator) {
        this.allocator = new RegisterAllocator(tacGenerator);
        this.instructions = new ArrayList<>();
        this.dataSegment = new HashMap<>();
        this.labelCounter = 0;
        this.currentFunction = null;
    }

    /**
     * Genera código MIPS completo
     */
    public String generate(List<TACInstruction> tacList) {
        StringBuilder code = new StringBuilder();

        // Generar segmento de datos
        code.append(generateDataSegment());
        code.append("\n");

        // Generar segmento de texto
        code.append(generateTextSegment(tacList));

        return code.toString();
    }

    /**
     * Genera el segmento de datos (.data)
     */
    private String generateDataSegment() {
        StringBuilder data = new StringBuilder();
        data.append(".data\n");

        for (Map.Entry<String, String> entry : dataSegment.entrySet()) {
            data.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return data.toString();
    }

    /**
     * Genera el segmento de texto (.text)
     */
    private String generateTextSegment(List<TACInstruction> tacList) {
        StringBuilder text = new StringBuilder();
        text.append(".text\n");
        text.append(".globl main\n\n");

        for (TACInstruction tac : tacList) {
            allocator.advanceLine(); // Avanzar contador de línea
            generateInstruction(tac);
        }

        // Agregar todas las instrucciones generadas
        for (MIPSInstruction instr : instructions) {
            text.append(instr.toString()).append("\n");
        }

        return text.toString();
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

            case LABEL:
                generateLabel(tac);
                break;

            case GOTO:
                generateGoto(tac);
                break;

            case IF_GOTO:
                //TODO generateIfGoto(tac);
                break;

            case CALL:
                generateCall(tac);
                break;

            case ASSIGN_CALL:
                //TODO generateAssignCall(tac);
                break;

            case NEW:
                //TODO generateNew(tac);
                break;

            case RETURN:
                generateReturn(tac);
                break;

            case END:
                generateFunctionEpilog(tac);
                break;

            case TRY_BEGIN:
                //TODO generateTryBegin(tac);
                break;

            case TRY_END:
                //TODO generateTryEnd(tac);
                break;

            default:
                instructions.add(MIPSInstruction.comment("Unsupported OpType: " + opType));
                break;
        }
    }

    /**
     * Genera asignación: x = y
     */
    private void generateAssignment(TACInstruction tac) {
        String dest = tac.getResult();
        String src = tac.getArg1();

        String destReg = allocator.getReg(dest);

        if (isImmediate(src)) {
            // li $dest, immediate
            int value = Integer.parseInt(src);
            instructions.add(MIPSInstruction.li(destReg, value));
        } else {
            String srcReg = allocator.getReg(src);
            // move $dest, $src
            instructions.add(MIPSInstruction.move(destReg, srcReg));
        }

        allocator.markDirty(destReg);
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
            // Operación inmediata: addi, subi, etc.
            int immediate = Integer.parseInt(arg2);

            if (op.equals("+")) {
                instructions.add(MIPSInstruction.typeI(OpCode.ADDI, resultReg, arg1Reg, immediate));
            } else if (op.equals("-")) {
                // subi es pseudo-instrucción, usar addi con negativo
                instructions.add(MIPSInstruction.typeI(OpCode.ADDI, resultReg, arg1Reg, -immediate));
            } else {
                // mul/div con inmediato requiere cargar a registro primero
                String tempReg = allocator.getReg("temp_imm");
                instructions.add(MIPSInstruction.li(tempReg, immediate));
                instructions.add(MIPSInstruction.typeR(opCode, resultReg, arg1Reg, tempReg));
                allocator.freeRegister(tempReg);
            }
        } else {
            String arg2Reg = allocator.getReg(arg2);
            // Operación entre registros
            instructions.add(MIPSInstruction.typeR(opCode, resultReg, arg1Reg, arg2Reg));
        }

        allocator.markDirty(resultReg);
    }

    /**
     * Genera operaciones unarias: x = -y, x = !y
     */
    private void generateUnary(TACInstruction tac) {
        String result = tac.getResult();
        String arg = tac.getArg1();
        String op = tac.getOperator();

        String resultReg = allocator.getReg(result);
        String argReg = allocator.getReg(arg);

        if (op.equals("-")) {
            // Negación: sub $result, $zero, $arg
            instructions.add(MIPSInstruction.typeR(OpCode.SUB, resultReg, "$zero", argReg));
        } else if (op.equals("!")) {
            // NOT lógico: seq $result, $arg, $zero (1 si arg == 0, 0 en otro caso)
            instructions.add(MIPSInstruction.typeR(OpCode.SEQ, resultReg, argReg, "$zero"));
        } else {
            instructions.add(MIPSInstruction.comment("Unknown unary operator: " + op));
        }

        allocator.markDirty(resultReg);
    }

    /**
     * Genera goto incondicional
     */
    private void generateGoto(TACInstruction tac) {
        String label = tac.getResult();
        instructions.add(MIPSInstruction.jump(OpCode.J, label));
    }

    /**
     * Genera if condicional: if condition goto label
     */
    private void generateIf(TACInstruction tac) {
        String condition = tac.getArg1();
        String label = tac.getResult();

        String condReg = allocator.getReg(condition);

        // if (condition != 0) goto label
        instructions.add(MIPSInstruction.branchUnary(OpCode.BNEZ, condReg, label));
    }

    /**
     * Genera llamada a función
     */
    private void generateCall(TACInstruction tac) {
        String functionName = tac.getArg1();

        // Guardar registros temporales antes de llamada
        allocator.saveTemporaries();

        // jal function
        instructions.add(MIPSInstruction.jump(OpCode.JAL, functionName));

        // Si hay resultado, moverlo de $v0
        if (tac.getResult() != null && !tac.getResult().isEmpty()) {
            String resultReg = allocator.getReg(tac.getResult());
            instructions.add(MIPSInstruction.move(resultReg, "$v0"));
            allocator.markDirty(resultReg);
        }
    }

    /**
     * Genera return
     */
    private void generateReturn(TACInstruction tac) {
        if (tac.getResult() != null && !tac.getResult().isEmpty()) {
            String returnValue = tac.getResult();
            String returnReg = allocator.getReg(returnValue);

            // Mover valor de retorno a $v0
            instructions.add(MIPSInstruction.move("$v0", returnReg));
        }

        // Saltar al epílogo de la función
        if (currentFunction != null) {
            instructions.add(MIPSInstruction.jump(OpCode.J, currentFunction + "_epilog"));
        } else {
            instructions.add(MIPSInstruction.jump(OpCode.J, "epilog"));
        }
    }

    /**
     * Genera parámetro para llamada
     */
    private void generateParam(TACInstruction tac) {
        String param = tac.getResult();
        String paramNumStr = tac.getArg1();

        int paramNum = 0;
        try {
            paramNum = Integer.parseInt(paramNumStr);
        } catch (NumberFormatException e) {
            paramNum = 0;
        }

        String paramReg = allocator.getReg(param);

        if (paramNum < 4) {
            // Usar registros $a0-$a3
            String argReg = "$a" + paramNum;
            instructions.add(MIPSInstruction.move(argReg, paramReg));
        } else {
            // Usar stack para parámetros adicionales
            int offset = (paramNum - 4) * 4;
            instructions.add(MIPSInstruction.loadStore(OpCode.SW, paramReg, offset + "($sp)"));
        }
    }

    /**
     * Genera etiqueta
     */
    private void generateLabel(TACInstruction tac) {
        String label = tac.getResult();
        instructions.add(MIPSInstruction.label(label));
    }

    /**
     * Genera prólogo de función
     */
    private void generateFunctionProlog(TACInstruction tac) {
        currentFunction = tac.getResult();

        instructions.add(MIPSInstruction.label(currentFunction));

        // Guardar $ra y $fp
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", -8));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$ra", "4($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.SW, "$fp", "0($sp)"));
        instructions.add(MIPSInstruction.move("$fp", "$sp"));

        // Reservar espacio para variables locales si es necesario
        int localSpace = calculateLocalSpace();
        if (localSpace > 0) {
            instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", -localSpace));
        }

        // Reiniciar allocator para nueva función
        allocator.reset();
    }

    /**
     * Genera epílogo de función
     */
    private void generateFunctionEpilog(TACInstruction tac) {
        String epilogLabel = currentFunction != null ? currentFunction + "_epilog" : "epilog";
        instructions.add(MIPSInstruction.label(epilogLabel));

        // Sincronizar registros dirty antes de salir
        allocator.flushAll();

        // Liberar espacio de variables locales
        instructions.add(MIPSInstruction.move("$sp", "$fp"));

        // Restaurar $fp y $ra
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$fp", "0($sp)"));
        instructions.add(MIPSInstruction.loadStore(OpCode.LW, "$ra", "4($sp)"));
        instructions.add(MIPSInstruction.typeI(OpCode.ADDI, "$sp", "$sp", 8));

        // Retornar
        instructions.add(MIPSInstruction.jumpReg("$ra"));
    }

    /**
     * Determina si un valor es inmediato (constante numérica)
     */
    private boolean isImmediate(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Obtiene la operación MIPS para operaciones aritméticas
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
     * Obtiene la operación MIPS para comparaciones (branches)
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
     * Obtiene la operación MIPS para comparaciones (set)
     */
    private OpCode getMipsComparisonSetOp(String op) {
        switch (op) {
            case "<": return OpCode.SLT;
            case ">": return OpCode.SGT;
            case "<=": return OpCode.SLE;
            case ">=": return OpCode.SGE;
            case "==": return OpCode.SEQ;
            case "!=": return OpCode.SNE;
            default: return OpCode.SEQ;
        }
    }

    /**
     * Genera una etiqueta temporal única
     */
    private String generateTempLabel() {
        return "L" + (labelCounter++);
    }

    /**
     * Calcula espacio necesario para variables locales
     */
    private int calculateLocalSpace() {
        // Simplificado: 16 palabras (64 bytes) para variables locales
        return 64;
    }

    /**
     * Agrega una variable al segmento de datos
     */
    public void addDataVariable(String name, String value) {
        dataSegment.put(name, value);
    }

    /**
     * Obtiene las instrucciones generadas (incluye las del allocator)
     */
    public List<MIPSInstruction> getInstructions() {
        List<MIPSInstruction> allInstructions = new ArrayList<>();
        allInstructions.addAll(instructions);
        allInstructions.addAll(allocator.getInstructions());
        return allInstructions;
    }

    /**
     * Obtiene el allocator de registros
     */
    public RegisterAllocator getAllocator() {
        return allocator;
    }

    /**
     * Obtiene contador de registros usados
     */
    public int getUsedRegistersCount() {
        // Contar cuántos registros tienen variables asignadas
        int count = 0;
        allocator.printState(); // Debug
        return count; // Simplificado
    }

    /**
     * Agrega variables globales al segmento de datos del generador MIPS
     */
    public void addGlobalVariables() {
        // Agregar strings comunes para syscalls
        addDataVariable("newline", ".asciiz \"\\n\"");
        addDataVariable("space", ".asciiz \" \"");
        addDataVariable("true_str", ".asciiz \"true\"");
        addDataVariable("false_str", ".asciiz \"false\"");
    }
}