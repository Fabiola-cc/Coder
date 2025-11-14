package com.fmd;

import com.fmd.modules.Symbol;
import com.fmd.modules.MIPSInstruction;
import com.fmd.modules.Register;
import java.util.*;

/**
 * Asignador de Registros para MIPS (Actualizado con clase Register)
 */
public class RegisterAllocator {

    // REGISTROS DISPONIBLES (usando objetos Register)
    private static final Register[] TEMP_REGISTERS = {
            Register.T0, Register.T1, Register.T2, Register.T3,
            Register.T4, Register.T5, Register.T6, Register.T7
    };

    private static final Register[] SAVED_REGISTERS = {
            Register.S0, Register.S1, Register.S2, Register.S3,
            Register.S4, Register.S5, Register.S6, Register.S7
    };

    // ESTADO DEL ALLOCATOR
    private Map<Register, RegisterDescriptor> registerState;    // registro -> descriptor
    private Map<String, Register> variableToRegister;           // variable -> registro actual
    private Stack<Register> freeRegisters;                      // registros disponibles
    private Map<String, Integer> lastUse;                       // variable -> última línea usada
    private int currentLine;                                    // línea TAC actual

    // Referencia a tabla de símbolos (para offsets)
    private TACGenerator tacGenerator;

    // Instrucciones MIPS generadas (spills)
    private List<MIPSInstruction> instructions;

    // CONSTRUCTOR
    public RegisterAllocator(TACGenerator tacGenerator) {
        this.tacGenerator = tacGenerator;
        this.registerState = new HashMap<>();
        this.variableToRegister = new HashMap<>();
        this.freeRegisters = new Stack<>();
        this.lastUse = new HashMap<>();
        this.currentLine = 0;
        this.instructions = new ArrayList<>();

        // Inicializar registros temporales como libres
        for (int i = TEMP_REGISTERS.length - 1; i >= 0; i--) {
            Register reg = TEMP_REGISTERS[i];
            freeRegisters.push(reg);
            registerState.put(reg, new RegisterDescriptor(reg.getName()));
        }

        // Inicializar registros saved (para variables persistentes)
        for (Register reg : SAVED_REGISTERS) {
            registerState.put(reg, new RegisterDescriptor(reg.getName()));
        }
    }

    // METODO PRINCIPAL: getReg()
    /**
     * ALGORITMO getReg() - Asigna un registro para una variable
     * @param variable Nombre de la variable o temporal
     * @return Nombre del registro MIPS asignado (String para compatibilidad)
     */
    public String getReg(String variable) {
        Register reg = allocateRegister(variable);
        return reg.getName();
    }

    /**
     * Versión que retorna objeto Register
     */
    public Register allocateRegister(String variable) {
        // PASO 1: Si ya tiene registro asignado, reutilizarlo
        if (variableToRegister.containsKey(variable)) {
            Register reg = variableToRegister.get(variable);
            lastUse.put(variable, currentLine);
            return reg;
        }

        // PASO 2: Si hay registros libres, tomar uno
        if (!freeRegisters.isEmpty()) {
            Register reg = freeRegisters.pop();
            assignRegister(variable, reg);
            return reg;
        }

        // PASO 3: ALGORITMO DE DESALOJO (spilling)
        Register victim = selectVictim();
        spillRegister(victim);
        assignRegister(variable, victim);
        return victim;
    }

    // ASIGNACIÓN Y LIBERACIÓN
    /**
     * Asigna un registro a una variable
     */
    private void assignRegister(String variable, Register register) {
        RegisterDescriptor desc = registerState.get(register);
        desc.assign(variable);
        variableToRegister.put(variable, register);
        lastUse.put(variable, currentLine);
    }

    /**
     * Libera un registro
     */
    public void freeRegister(String registerName) {
        Register reg = findRegister(registerName);
        if (reg != null) {
            freeRegister(reg);
        }
    }

    public void freeRegister(Register register) {
        RegisterDescriptor desc = registerState.get(register);
        if (desc != null && desc.getVariable() != null) {
            if (desc.isDirty()) {
                spillRegister(register);
            } else {
                variableToRegister.remove(desc.getVariable());
                desc.free();
                freeRegisters.push(register);
            }
        }
    }

    // SPILLING (DESALOJO A MEMORIA)
    /**
     * Guarda un registro en memoria
     */
    private void spillRegister(Register register) {
        RegisterDescriptor desc = registerState.get(register);

        if (desc.isDirty()) {
            String variable = desc.getVariable();
            int offset = getVariableOffset(variable);

            MIPSInstruction store = MIPSInstruction.loadStore(
                    MIPSInstruction.OpCode.SW,
                    register.getName(),
                    offset + "($sp)"
            );
            store.setComment("spill " + variable);
            instructions.add(store);

            System.out.println("  [SPILL] " + variable + " -> memoria (offset " + offset + ")");
        }

        variableToRegister.remove(desc.getVariable());
        desc.free();
    }

    /**
     * Selecciona registro víctima para desalojar
     */
    private Register selectVictim() {
        Register victim = null;
        int maxDistance = -1;

        for (Register reg : TEMP_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc.getVariable() != null) {
                String var = desc.getVariable();

                // Priorizar temporales para spill
                if (var.startsWith("t")) {
                    return reg;
                }

                int distance = getNextUse(var);
                if (distance > maxDistance) {
                    maxDistance = distance;
                    victim = reg;
                }
            }
        }

        return victim != null ? victim : TEMP_REGISTERS[0];
    }

    /**
     * Estima cuándo se usará una variable de nuevo
     */
    private int getNextUse(String variable) {
        Integer last = lastUse.get(variable);
        if (last == null) return Integer.MAX_VALUE;
        return currentLine - last;
    }

    // ACCESO A MEMORIA (LOAD/STORE)
    /**
     * Obtiene el offset en el frame para una variable
     */
    private int getVariableOffset(String variable) {
        Symbol sym = tacGenerator.getSymbol(variable);

        if (sym != null && sym.getOffset() >= 0) {
            return sym.getOffset();
        }

        System.err.println("WARNING: Variable " + variable + " sin offset, usando 0");
        return 0;
    }

    /**
     * Carga una variable de memoria a registro
     */
    public void loadVariable(String variable, String registerName) {
        Register register = findRegister(registerName);
        if (register != null) {
            loadVariable(variable, register);
        }
    }

    public void loadVariable(String variable, Register register) {
        int offset = getVariableOffset(variable);

        MIPSInstruction load = MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.LW,
                register.getName(),
                offset + "($sp)"
        );
        load.setComment("load " + variable);
        instructions.add(load);

        RegisterDescriptor desc = registerState.get(register);
        desc.assign(variable);
        desc.setClean();
        variableToRegister.put(variable, register);
    }

    /**
     * Guarda un registro en memoria
     */
    public void storeVariable(String registerName, String variable) {
        Register register = findRegister(registerName);
        if (register != null) {
            storeVariable(register, variable);
        }
    }

    public void storeVariable(Register register, String variable) {
        int offset = getVariableOffset(variable);

        MIPSInstruction store = MIPSInstruction.loadStore(
                MIPSInstruction.OpCode.SW,
                register.getName(),
                offset + "($sp)"
        );
        store.setComment("store " + variable);
        instructions.add(store);

        RegisterDescriptor desc = registerState.get(register);
        desc.setClean();
    }

    // DIRTY BIT MANAGEMENT
    /**
     * Marca un registro como "dirty"
     */
    public void markDirty(String registerName) {
        Register register = findRegister(registerName);
        if (register != null) {
            markDirty(register);
        }
    }

    public void markDirty(Register register) {
        RegisterDescriptor desc = registerState.get(register);
        if (desc != null) {
            desc.setDirty();
        }
    }

    /**
     * Sincroniza todos los registros dirty con memoria
     */
    public void flushAll() {
        for (Register reg : registerState.keySet()) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc.isDirty()) {
                spillRegister(reg);
            }
        }
    }

    // CONTEXT MANAGEMENT
    /**
     * Guarda todos los registros $t antes de una llamada
     */
    public void saveTemporaries() {
        for (Register reg : TEMP_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc.getVariable() != null && desc.isDirty()) {
                spillRegister(reg);
            }
        }
    }

    /**
     * Guarda todos los registros $s
     */
    public void saveSavedRegisters() {
        for (Register reg : SAVED_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            if (desc.getVariable() != null) {
                spillRegister(reg);
            }
        }
    }

    // UTILIDADES
    /**
     * Encuentra un objeto Register por su nombre
     */
    private Register findRegister(String name) {
        for (Register reg : TEMP_REGISTERS) {
            if (reg.getName().equals(name)) {
                return reg;
            }
        }
        for (Register reg : SAVED_REGISTERS) {
            if (reg.getName().equals(name)) {
                return reg;
            }
        }
        // Si no está en los arrays, crear uno nuevo
        return new Register(name);
    }

    /**
     * Avanza el contador de línea
     */
    public void advanceLine() {
        currentLine++;
    }

    /**
     * Obtiene las instrucciones MIPS generadas (spills)
     */
    public List<MIPSInstruction> getInstructions() {
        return instructions;
    }

    /**
     * Reinicia el estado del allocator
     */
    public void reset() {
        variableToRegister.clear();
        lastUse.clear();
        freeRegisters.clear();
        instructions.clear();
        currentLine = 0;

        for (int i = TEMP_REGISTERS.length - 1; i >= 0; i--) {
            Register reg = TEMP_REGISTERS[i];
            freeRegisters.push(reg);
            registerState.get(reg).free();
        }
    }

    /**
     * Obtiene variables activas
     */
    public Set<String> getActiveVariables() {
        return new HashSet<>(variableToRegister.keySet());
    }

    /**
     * Debug: imprime estado actual de registros
     */
    public void printState() {
        System.out.println("\n=== ESTADO DE REGISTROS ===");
        for (Register reg : TEMP_REGISTERS) {
            RegisterDescriptor desc = registerState.get(reg);
            System.out.println(reg.getName() + ": " + desc);
        }
        System.out.println("Variables asignadas: " + variableToRegister);
        System.out.println("Registros libres: " + freeRegisters.size());
    }
}