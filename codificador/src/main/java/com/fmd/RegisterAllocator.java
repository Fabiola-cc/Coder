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
        this.nextTempOffset = 1000;

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
        desc.setDirty();
        variableToRegister.put(variable, register);
        lastUse.put(variable, currentLine);
    }


    /**
     * Libera un registro (lo marca como disponible)
     */
    public void freeRegister(String register) {
        RegisterDescriptor desc = registerState.get(register);
        if (desc == null) {
            if (!freeRegisters.contains(register)) freeRegisters.push(register);
            return;
        }
        String var = desc.getVariable();
        if (var != null) {
            if (desc.isDirty()) {
                // spillRegister liberará y pondrá el registro en freeRegisters
                spillRegister(register);
                return;
            } else {
                variableToRegister.remove(var);
                desc.free();
                if (!freeRegisters.contains(register)) freeRegisters.push(register);
            }
        } else {
            // ya libre
            desc.free();
            if (!freeRegisters.contains(register)) freeRegisters.push(register);
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
        if (desc == null || desc.getVariable() == null) {
            // nada que hacer
            desc = (desc == null) ? new RegisterDescriptor(register) : desc;
            desc.free();
            if (!freeRegisters.contains(register)) freeRegisters.push(register);
            return;
        }

        String variable = desc.getVariable();
        int offset = getVariableOffset(variable);

        if (offset < 0) {
            // No podemos spilled una literal o variable sin offset válido.
            System.out.println("[INFO] No se genera spill para '" + variable + "' (no requiere offset).");
            return;
        } else {
            // Generar instrucción MIPS: sw $reg, offset($sp)
            MIPSInstruction store = MIPSInstruction.loadStore(
                    MIPSInstruction.OpCode.SW,
                    register,
                    offset + "($sp)"
            );
            instructions.add(store);
            System.out.println("  [SPILL] " + variable + " -> memoria (offset " + offset + ")");
        }

        // Liberar el registro (asegurar consistencia)
        variableToRegister.remove(variable);
        desc.free();
        if (!freeRegisters.contains(register)) freeRegisters.push(register);
    }

    private boolean needsStackSlot(String var) {
        if (var == null) return false;

        // Strings literales
        if (var.startsWith("\"") && var.endsWith("\""))
            return false;

        // Literales numéricos
        if (var.matches("\\d+"))
            return false;

        // Accesos a arreglo con índice variable
        if (var.matches(".*\\[.*\\].*"))
            return false;

        // Campos de objeto
        if (var.startsWith("this."))
            return false;

        // Variables de excepción
        if (var.equals("exception") || var.equals("err"))
            return false;

        // Si no cae en lo anterior → sí necesita offset
        return true;
    }



    /**
     * Selecciona registro víctima para desalojar
     * Estrategia: Furthest Use (el que se usa más lejos en el futuro)
     */
    private String selectVictim() {
        // Priorizar temporales (t#) ya en registers
        for (String reg : TEMP_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc != null && desc.getVariable() != null && desc.getVariable().startsWith("t")) {
                return reg;
            }
        }

        String victim = null;
        int maxAge = -1;
        for (String reg : TEMP_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc != null && desc.getVariable() != null) {
                String var = desc.getVariable();
                Integer last = lastUse.get(var);
                int age = (last == null) ? Integer.MAX_VALUE : currentLine - last;
                if (age > maxAge) {
                    maxAge = age;
                    victim = reg;
                }
            }
        }
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
        if (variable == null) return -1;

        // 1. Literales de cadena: no deben pedirse offsets
        if (variable.startsWith("\"") && variable.endsWith("\"")) {
            return -1;
        }

        // 2. Temporales t<number>
        if (variable.matches("^t\\d+$")) {
            if (!tempOffsets.containsKey(variable)) {
                tempOffsets.put(variable, nextTempOffset);
                nextTempOffset += 4;
            }
            return tempOffsets.get(variable);
        }

        // 3. Acceso a array como name[CONST]
        if (variable.contains("[")) {
            int idxOpen = variable.indexOf('[');
            int idxClose = variable.indexOf(']');
            if (idxClose > idxOpen) {
                String baseName = variable.substring(0, idxOpen);
                String indexPart = variable.substring(idxOpen + 1, idxClose).trim();

                Symbol baseSym = tacGenerator.getSymbol(baseName);
                if (baseSym == null) {
                    System.err.println("WARNING: Array base '" + baseName + "' no encontrada en tabla de símbolos");
                    return -1;
                }
                int baseOffset = baseSym.getOffset();
                // índice constante
                try {
                    int idx = Integer.parseInt(indexPart);
                    return baseOffset + idx * 4; // asumiendo 4 bytes por elemento
                } catch (NumberFormatException e) {
                    // índice no constante (p. ej. numbers[t2]) -> devolvemos baseOffset para que el código calcule dirección efectiva
                    System.err.println("WARNING: Index no constante en '" + variable + "'. Devolveré base (" + baseOffset + "), requiere cálculo de dirección.");
                    return baseOffset;
                }
            }
            return -1;
        }

        // 4. Acceso a miembro this.name (o obj.member)
        if (variable.contains(".")) {
            String[] parts = variable.split("\\.");
            if (parts.length == 2) {
                String owner = parts[0];
                String member = parts[1];
                // Intentar buscar símbolo calificado en tabla (checar si tacGenerator tiene soporte)
                Symbol sym = tacGenerator.getSymbol(member);
                if (sym != null && sym.getOffset() >= 0) {
                    return sym.getOffset();
                } else {
                    return -1;
                }
            }
            return -1;
        }

        // 5. Variable normal
        Symbol sym = tacGenerator.getSymbol(variable);
        if (sym != null && sym.getOffset() >= 0) {
            return sym.getOffset();
        }

        // 6. No encontrada -> advertencia
        return -1;
    }


    /**
     * Carga una variable de memoria a registro
     */
    public void loadVariable(String variable, String register) {
        int offset = getVariableOffset(variable);
        if (offset < 0) {
            System.err.println("ERROR: Intento de cargar '" + variable + "' con offset inválido (" + offset + "). Operación omitida.");
            return;
        }

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

    public void storeVariable(String register, String variable) {
        int offset = getVariableOffset(variable);
        if (offset < 0) {
            System.err.println("ERROR: Intento de guardar '" + variable + "' con offset inválido (" + offset + "). Operación omitida.");
            return;
        }

        MIPSInstruction store = MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.SW,
                register,
                offset + "($sp)"
        );
        instructions.add(store);

        // Marcar como limpio (ya sincronizado con memoria)
        RegisterDescriptor desc = registerState.get(register);
        if (desc != null) desc.setClean();
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