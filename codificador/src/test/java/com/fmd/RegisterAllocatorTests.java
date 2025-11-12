package com.fmd;

import com.fmd.modules.MIPSInstruction;
import com.fmd.modules.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests para RegisterAllocator")
public class RegisterAllocatorTests {

    private TACGenerator tacGen;
    private RegisterAllocator allocator;

    @BeforeEach
    void setUp() {
        tacGen = createMockTACGenerator();
        allocator = new RegisterAllocator(tacGen);
    }

    // ============================================
    // TESTS BÁSICOS
    // ============================================

    @Test
    @DisplayName("Asignación básica de registros diferentes")
    void testBasicAllocation() {
        String reg1 = allocator.getReg("x");
        String reg2 = allocator.getReg("y");
        String reg3 = allocator.getReg("z");

        assertNotEquals(reg1, reg2, "x e y deben tener registros diferentes");
        assertNotEquals(reg2, reg3, "y y z deben tener registros diferentes");
        assertNotEquals(reg1, reg3, "x y z deben tener registros diferentes");

        assertTrue(reg1.startsWith("$t"), "Debe usar registros $t");
        assertTrue(reg2.startsWith("$t"), "Debe usar registros $t");
        assertTrue(reg3.startsWith("$t"), "Debe usar registros $t");
    }

    @Test
    @DisplayName("Reutilización del mismo registro para la misma variable")
    void testRegisterReuse() {
        String reg1 = allocator.getReg("x");
        String reg2 = allocator.getReg("x");

        assertEquals(reg1, reg2, "La misma variable debe obtener el mismo registro");
    }

    @Test
    @DisplayName("Todos los registros temporales son diferentes")
    void testAllTempsUnique() {
        Set<String> registers = new HashSet<>();

        // Asignar 8 variables (máximo de $t0-$t7)
        for (int i = 1; i <= 8; i++) {
            String reg = allocator.getReg("var" + i);
            registers.add(reg);
        }

        assertEquals(8, registers.size(), "Debe usar 8 registros diferentes");
    }

    // ============================================
    // TESTS DE SPILLING
    // ============================================

    @Test
    @DisplayName("Spilling cuando se agotan los registros")
    void testSpillingWhenOutOfRegisters() {
        // Asignar 8 variables (llenar todos los $t)
        for (int i = 1; i <= 8; i++) {
            String reg = allocator.getReg("var" + i);
            allocator.markDirty(reg);
        }

        // La 9na variable debe provocar spill
        String reg9 = allocator.getReg("var9");

        assertNotNull(reg9, "Debe asignar un registro incluso después de spilling");

        List<MIPSInstruction> instrs = allocator.getInstructions();
        assertFalse(instrs.isEmpty(), "Debe generar instrucciones de spill");

        // Verificar que hay al menos un SW (spill)
        boolean hasSpill = instrs.stream()
                .anyMatch(i -> i.getOpcode() == MIPSInstruction.OpCode.SW);
        assertTrue(hasSpill, "Debe generar instrucción SW para spill");
    }

    @Test
    @DisplayName("Múltiples spills generan múltiples SW")
    void testMultipleSpills() {
        // Asignar 10 variables (provocar 2 spills)
        for (int i = 1; i <= 10; i++) {
            String reg = allocator.getReg("var" + i);
            allocator.markDirty(reg);
        }

        List<MIPSInstruction> instrs = allocator.getInstructions();
        long spillCount = instrs.stream()
                .filter(i -> i.getOpcode() == MIPSInstruction.OpCode.SW)
                .count();

        assertTrue(spillCount >= 2, "Debe hacer al menos 2 spills");
    }

    @Test
    @DisplayName("No hace spill si el registro no está dirty")
    void testNoSpillIfClean() {
        String reg = allocator.getReg("x");
        // No marcar como dirty

        allocator.freeRegister(reg);

        List<MIPSInstruction> instrs = allocator.getInstructions();
        assertTrue(instrs.isEmpty(), "No debe generar SW si el registro está limpio");
    }

    // ============================================
    // TESTS DE DIRTY BIT
    // ============================================

    @Test
    @DisplayName("Dirty bit causa spill al liberar")
    void testDirtyBitCausesSpill() {
        String reg = allocator.getReg("x");
        allocator.markDirty(reg);

        allocator.freeRegister(reg);

        List<MIPSInstruction> instrs = allocator.getInstructions();
        assertFalse(instrs.isEmpty(), "Debe generar SW cuando está dirty");

        MIPSInstruction spill = instrs.get(0);
        assertEquals(MIPSInstruction.OpCode.SW, spill.getOpcode());
        assertTrue(spill.getComment().contains("x"), "Comentario debe mencionar la variable");
    }

    @Test
    @DisplayName("Load marca registro como clean")
    void testLoadMarksClean() {
        String reg = allocator.getReg("x");
        allocator.loadVariable("x", reg);

        // Liberar sin marcar dirty
        allocator.freeRegister(reg);

        // Solo debe haber el LW inicial, no SW
        List<MIPSInstruction> instrs = allocator.getInstructions();
        long swCount = instrs.stream()
                .filter(i -> i.getOpcode() == MIPSInstruction.OpCode.SW)
                .count();

        assertEquals(0, swCount, "No debe hacer SW si solo se hizo load");
    }

    // ============================================
    // TESTS DE LOAD/STORE
    // ============================================

    @Test
    @DisplayName("loadVariable genera instrucción LW")
    void testLoadVariable() {
        String reg = allocator.getReg("x");
        allocator.loadVariable("x", reg);

        List<MIPSInstruction> instrs = allocator.getInstructions();
        assertEquals(1, instrs.size(), "Debe generar exactamente 1 instrucción");

        MIPSInstruction lw = instrs.get(0);
        assertEquals(MIPSInstruction.OpCode.LW, lw.getOpcode());
        assertTrue(lw.getImmediate().contains("$sp"), "Debe usar stack pointer");
    }

    @Test
    @DisplayName("storeVariable genera instrucción SW")
    void testStoreVariable() {
        String reg = allocator.getReg("x");
        allocator.storeVariable(reg, "x");

        List<MIPSInstruction> instrs = allocator.getInstructions();
        assertEquals(1, instrs.size(), "Debe generar exactamente 1 instrucción");

        MIPSInstruction sw = instrs.get(0);
        assertEquals(MIPSInstruction.OpCode.SW, sw.getOpcode());
    }

    @Test
    @DisplayName("Offset correcto para variables en memoria")
    void testVariableOffsets() {
        allocator.loadVariable("x", "$t0");  // offset 0
        allocator.loadVariable("y", "$t1");  // offset 4
        allocator.loadVariable("z", "$t2");  // offset 8

        List<MIPSInstruction> instrs = allocator.getInstructions();
        assertEquals(3, instrs.size());

        assertTrue(instrs.get(0).getImmediate().contains("0($sp)"));
        assertTrue(instrs.get(1).getImmediate().contains("4($sp)"));
        assertTrue(instrs.get(2).getImmediate().contains("8($sp)"));
    }

    // ============================================
    // TESTS DE EXPRESIONES COMPLEJAS
    // ============================================

    @Test
    @DisplayName("Expresión aritmética simple: a = b + c")
    void testSimpleArithmetic() {
        String rb = allocator.getReg("b");
        String rc = allocator.getReg("c");
        String result = allocator.getReg("result");

        // Simular: result = b + c
        allocator.markDirty(result);

        assertNotNull(rb);
        assertNotNull(rc);
        assertNotNull(result);

        // Verificar que son diferentes
        assertNotEquals(rb, rc);
        assertNotEquals(rc, result);
    }

    @Test
    @DisplayName("Expresión compleja: a = (b + c) * (d - e)")
    void testComplexExpression() {
        // Primera parte: t1 = b + c
        allocator.getReg("b");
        allocator.getReg("c");
        String t1 = allocator.getReg("t1");
        allocator.markDirty(t1);

        // Segunda parte: t2 = d - e
        allocator.getReg("d");
        allocator.getReg("e");
        String t2 = allocator.getReg("t2");
        allocator.markDirty(t2);

        // Resultado: a = t1 * t2
        String rt1 = allocator.getReg("t1");
        String rt2 = allocator.getReg("t2");
        String a = allocator.getReg("a");

        assertEquals(t1, rt1, "t1 debe reutilizarse");
        assertEquals(t2, rt2, "t2 debe reutilizarse");
        assertNotNull(a);
    }

    // ============================================
    // TESTS DE FUNCIONES
    // ============================================

    @Test
    @DisplayName("saveTemporaries guarda todos los registros dirty")
    void testSaveTemporaries() {
        // Asignar varios temporales y marcarlos dirty
        for (int i = 1; i <= 4; i++) {
            String reg = allocator.getReg("t" + i);
            allocator.markDirty(reg);
        }

        allocator.saveTemporaries();

        List<MIPSInstruction> instrs = allocator.getInstructions();
        long swCount = instrs.stream()
                .filter(i -> i.getOpcode() == MIPSInstruction.OpCode.SW)
                .count();

        assertEquals(4, swCount, "Debe guardar todos los temporales dirty");
    }

    @Test
    @DisplayName("flushAll sincroniza todos los registros")
    void testFlushAll() {
        // Asignar varias variables y marcarlas dirty
        for (int i = 1; i <= 3; i++) {
            String reg = allocator.getReg("var" + i);
            allocator.markDirty(reg);
        }

        allocator.flushAll();

        List<MIPSInstruction> instrs = allocator.getInstructions();
        long swCount = instrs.stream()
                .filter(i -> i.getOpcode() == MIPSInstruction.OpCode.SW)
                .count();

        assertTrue(swCount >= 3, "Debe hacer flush de todas las variables");
    }

    // ============================================
    // TESTS DE RESET
    // ============================================

    @Test
    @DisplayName("reset limpia el estado del allocator")
    void testReset() {
        // Usar algunos registros
        allocator.getReg("x");
        allocator.getReg("y");
        allocator.getReg("z");

        allocator.reset();

        // Después del reset, debe poder asignar de nuevo
        String reg = allocator.getReg("x");
        assertNotNull(reg, "Debe poder asignar después de reset");

        List<MIPSInstruction> instrs = allocator.getInstructions();
        assertTrue(instrs.isEmpty(), "Las instrucciones deben limpiarse");
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private TACGenerator createMockTACGenerator() {
        Map<String, SemanticVisitor.Entorno> scopeTable = new HashMap<>();
        SemanticVisitor.Entorno root = new SemanticVisitor.Entorno(null, "0");
        scopeTable.put("0", root);

        // Variables comunes
        String[] vars = {"x", "y", "z", "a", "b", "c", "d", "e", "result"};
        for (int i = 0; i < vars.length; i++) {
            addMockSymbol(root, vars[i], "integer", i * 4);
        }

        // Temporales
        for (int i = 1; i <= 20; i++) {
            addMockSymbol(root, "t" + i, "integer", (vars.length + i) * 4);
            addMockSymbol(root, "var" + i, "integer", (vars.length + 20 + i) * 4);
        }

        TACGenerator tacGen = new TACGenerator(scopeTable);
        tacGen.setCurrentScopeLine("0");
        return tacGen;
    }

    private void addMockSymbol(SemanticVisitor.Entorno entorno,
                               String name, String type, int offset) {
        Symbol sym = new Symbol(name, Symbol.Kind.VARIABLE, type, null, 1, 1, true);
        sym.setOffset(offset);
        sym.setSize(4);
        entorno.agregar(sym);
    }
}