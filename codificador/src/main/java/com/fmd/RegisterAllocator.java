package com.fmd;

import com.fmd.modules.Symbol;
import com.fmd.modules.MIPSInstruction;
import java.util.*;

/**
 * Asignador de Registros para MIPS
 *
 * RESPONSABILIDADES:
 * - Asignar registros a variables/temporales usando getReg()
 * - Mantener descriptores de registros (qué contiene cada uno)
 * - Implementar spilling cuando no hay registros disponibles
 * - Tracking de dirty bits para optimizar stores
 */
public class RegisterAllocator {

    // ============================================
    // REGISTROS DISPONIBLES
    // ============================================
    private static final String[] TEMP_REGISTERS = {
            "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7"
    };

    private static final String[] SAVED_REGISTERS = {
            "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"
    };

    // ============================================
    // ESTADO DEL ALLOCATOR
    // ============================================
    private Map<String, RegisterDescriptor> registerState;      // registro -> descriptor
    private Map<String, String> variableToRegister;             // variable -> registro actual
    private Stack<String> freeRegisters;                        // registros disponibles
    private Map<String, Integer> lastUse;                       // variable -> última línea usada
    private int currentLine;                                    // línea TAC actual

    // Referencia a tabla de símbolos (para offsets)
    private TACGenerator tacGenerator;

    // Instrucciones MIPS generadas
    private List<MIPSInstruction> instructions;

    // Offsets dinámicos para temporales (t1, t2, ...)
    private Map<String, Integer> tempOffsets;
    private int nextTempOffset;

    // ============================================
    // CONSTRUCTOR
    // ============================================
    public RegisterAllocator(TACGenerator tacGenerator) {
        this.tacGenerator = tacGenerator;
        this.registerState = new HashMap<>();
        this.variableToRegister = new HashMap<>();
        this.freeRegisters = new Stack<>();
        this.lastUse = new HashMap<>();
        this.currentLine = 0;
        this.instructions = new ArrayList<>();
        this.tempOffsets = new HashMap<>();
        this.nextTempOffset = 1000; // Empezar en offset 1000 para temporales

        // Inicializar registros temporales como libres
        for (int i = TEMP_REGISTERS.length - 1; i >= 0; i--) {
            freeRegisters.push(TEMP_REGISTERS[i]);
            registerState.put(TEMP_REGISTERS[i], new RegisterDescriptor(TEMP_REGISTERS[i]));
        }

        // Inicializar registros saved (para variables persistentes)
        for (String reg : SAVED_REGISTERS) {
            registerState.put(reg, new RegisterDescriptor(reg));
        }
    }

    // ============================================
    // MÉTODO PRINCIPAL: getReg()
    // ============================================
    /**
     * ALGORITMO getReg() - NÚCLEO DEL PROYECTO
     * Asigna un registro para una variable/temporal según el algoritmo del Dragon Book
     *
     * @param variable Nombre de la variable o temporal (ej: "t1", "x", "arr")
     * @return Registro MIPS asignado (ej: "$t0", "$s1")
     */
    public String getReg(String variable) {
        // PASO 1: Si ya tiene registro asignado, reutilizarlo
        if (variableToRegister.containsKey(variable)) {
            String reg = variableToRegister.get(variable);
            lastUse.put(variable, currentLine); // actualizar uso
            return reg;
        }

        // PASO 2: Si hay registros libres, tomar uno
        if (!freeRegisters.isEmpty()) {
            String reg = freeRegisters.pop();
            assignRegister(variable, reg);
            return reg;
        }

        // PASO 3: ALGORITMO DE DESALOJO (spilling)
        // No hay registros libres, hay que desalojar uno
        String victim = selectVictim();
        spillRegister(victim);
        assignRegister(variable, victim);
        return victim;
    }

    // ============================================
    // ASIGNACIÓN Y LIBERACIÓN
    // ============================================
    /**
     * Asigna un registro a una variable
     */
    private void assignRegister(String variable, String register) {
        RegisterDescriptor desc = registerState.get(register);
        desc.assign(variable);
        variableToRegister.put(variable, register);
        lastUse.put(variable, currentLine);
    }

    /**
     * Libera un registro (lo marca como disponible)
     */
    public void freeRegister(String register) {
        RegisterDescriptor desc = registerState.get(register);
        if (desc != null && desc.getVariable() != null) {
            // Si está dirty, hacer spill antes de liberar
            if (desc.isDirty()) {
                spillRegister(register);
            } else {
                // Solo limpiar sin spill
                variableToRegister.remove(desc.getVariable());
                desc.free();
                freeRegisters.push(register);
            }
        }
    }

    // ============================================
    // SPILLING (DESALOJO A MEMORIA)
    // ============================================
    /**
     * Guarda un registro en memoria (stack o frame)
     * Solo guarda si el registro está "dirty" (modificado)
     */
    private void spillRegister(String register) {
        RegisterDescriptor desc = registerState.get(register);

        if (desc.isDirty()) {
            String variable = desc.getVariable();
            int offset = getVariableOffset(variable);

            // Generar instrucción MIPS: sw $reg, offset($sp)
            MIPSInstruction store = MIPSInstruction.loadStore(
                    MIPSInstruction.OpCode.SW,
                    register,
                    offset + "($sp)"
            );

            instructions.add(store);

            System.out.println("  [SPILL] " + variable + " -> memoria (offset " + offset + ")");
        }

        // Liberar el registro
        variableToRegister.remove(desc.getVariable());
        desc.free();
    }

    /**
     * Selecciona registro víctima para desalojar
     * Estrategia: Furthest Use (el que se usa más lejos en el futuro)
     */
    private String selectVictim() {
        String victim = null;
        int maxDistance = -1;

        // Buscar el registro cuya variable se usa más lejos
        for (String reg : TEMP_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc.getVariable() != null) {
                String var = desc.getVariable();

                // Si es temporal (t1, t2...), priorizar para spill
                if (var.startsWith("t")) {
                    return reg;
                }

                // Calcular distancia al próximo uso
                int distance = getNextUse(var);
                if (distance > maxDistance) {
                    maxDistance = distance;
                    victim = reg;
                }
            }
        }

        // Si no encontró víctima, usar el primero disponible
        return victim != null ? victim : TEMP_REGISTERS[0];
    }

    /**
     * Estima cuándo se usará una variable de nuevo
     * (simplificado: usa lastUse como proxy)
     */
    private int getNextUse(String variable) {
        Integer last = lastUse.get(variable);
        if (last == null) return Integer.MAX_VALUE;
        return currentLine - last; // distancia desde último uso
    }

    // ============================================
    // ACCESO A MEMORIA (LOAD/STORE)
    // ============================================
    /**
     * Obtiene el offset en el frame para una variable
     * Usa la tabla de símbolos del TAC que ya tiene los offsets calculados
     */
    private int getVariableOffset(String variable) {
        // 1. Si es temporal (t1, t2...), asignar offset dinámico en el stack
        if (variable.startsWith("t") && variable.length() > 1
                && Character.isDigit(variable.charAt(1))) {

            // Usar un mapa temporal para offsets dinámicos
            if (!tempOffsets.containsKey(variable)) {
                tempOffsets.put(variable, nextTempOffset);
                nextTempOffset += 4; // siguiente offset (4 bytes por int)
            }
            return tempOffsets.get(variable);
        }

        // 2. Si es string literal, NO debería llegar aquí
        if (variable.startsWith("\"")) {
            System.err.println("ERROR: String literal '" + variable + "' no debe necesitar offset");
            return -1;
        }

        // 3. Si es acceso a array (numbers[0], numbers[t54]), parsear base
        if (variable.contains("[")) {
            String baseName = variable.substring(0, variable.indexOf('['));
            Symbol sym = tacGenerator.getSymbol(baseName);
            if (sym != null && sym.getOffset() >= 0) {
                return sym.getOffset();
            }
            // Si no encuentra la base, podría ser un temporal con array
            System.err.println("WARNING: Array access '" + variable + "' sin offset de base");
            return 0;
        }

        // 4. Si es acceso a miembro (this.name), parsear
        if (variable.contains(".")) {
            String[] parts = variable.split("\\.");
            if (parts.length == 2) {
                Symbol sym = tacGenerator.getSymbol(parts[1]); // nombre del miembro
                if (sym != null && sym.getOffset() >= 0) {
                    return sym.getOffset();
                }
            }
            System.err.println("WARNING: Member access '" + variable + "' sin offset");
            return 0;
        }

        // 5. Variable normal del programa
        Symbol sym = tacGenerator.getSymbol(variable);
        if (sym != null && sym.getOffset() >= 0) {
            return sym.getOffset();
        }

        // 6. Si no se encuentra, advertencia
        System.err.println("WARNING: Variable '" + variable + "' sin offset, usando 0");
        return 0;
    }

    /**
     * Carga una variable de memoria a registro
     */
    public void loadVariable(String variable, String register) {
        int offset = getVariableOffset(variable);

        MIPSInstruction load = MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.LW,
                register,
                offset + "($sp)"
        );

        instructions.add(load);

        // Actualizar descriptor
        RegisterDescriptor desc = registerState.get(register);
        desc.assign(variable);
        desc.setClean(); // recién cargado, no está dirty
        variableToRegister.put(variable, register);
    }

    /**
     * Guarda un registro en memoria
     */
    public void storeVariable(String register, String variable) {
        int offset = getVariableOffset(variable);

        MIPSInstruction store = MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.SW,
                register,
                offset + "($sp)"
        );
        instructions.add(store);

        // Marcar como limpio (ya sincronizado con memoria)
        RegisterDescriptor desc = registerState.get(register);
        desc.setClean();
    }

    // ============================================
    // DIRTY BIT MANAGEMENT
    // ============================================
    /**
     * Marca un registro como "dirty" (modificado, no sincronizado)
     */
    public void markDirty(String register) {
        RegisterDescriptor desc = registerState.get(register);
        if (desc != null) {
            desc.setDirty();
        }
    }

    /**
     * Sincroniza todos los registros dirty con memoria
     * (útil al final de bloques básicos o antes de llamadas)
     */
    public void flushAll() {
        for (String reg : registerState.keySet()) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc.isDirty()) {
                spillRegister(reg);
            }
        }
    }

    // ============================================
    // CONTEXT MANAGEMENT (para llamadas a función)
    // ============================================
    /**
     * Guarda todos los registros $t antes de una llamada
     */
    public void saveTemporaries() {
        for (String reg : TEMP_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc.getVariable() != null && desc.isDirty()) {
                spillRegister(reg);
            }
        }
    }

    /**
     * Guarda todos los registros $s (caller-saved en convención MIPS)
     */
    public void saveSavedRegisters() {
        for (String reg : SAVED_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc.getVariable() != null) {
                spillRegister(reg);
            }
        }
    }

    // ============================================
    // UTILIDADES
    // ============================================
    /**
     * Avanza el contador de línea (para algoritmo de próximo uso)
     */
    public void advanceLine() {
        currentLine++;
    }

    /**
     * Obtiene las instrucciones MIPS generadas
     */
    public List<MIPSInstruction> getInstructions() {
        return instructions;
    }

    /**
     * Reinicia el estado del allocator (para nueva función)
     */
    public void reset() {
        variableToRegister.clear();
        lastUse.clear();
        freeRegisters.clear();
        instructions.clear();
        tempOffsets.clear();
        currentLine = 0;
        nextTempOffset = 1000;

        // Reinicializar registros
        for (int i = TEMP_REGISTERS.length - 1; i >= 0; i--) {
            freeRegisters.push(TEMP_REGISTERS[i]);
            registerState.get(TEMP_REGISTERS[i]).free();
        }
    }

    /**
     * Debug: imprime estado actual de registros
     */
    public void printState() {
        System.out.println("\n=== ESTADO DE REGISTROS ===");
        for (String reg : TEMP_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            System.out.println(reg + ": " + desc);
        }
        System.out.println("Variables asignadas: " + variableToRegister);
        System.out.println("Registros libres: " + freeRegisters.size());
    }
}