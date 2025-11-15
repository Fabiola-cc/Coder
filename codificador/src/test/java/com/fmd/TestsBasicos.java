package com.fmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el generador de código MIPS
 */
class TestsBasicos {

    private TestInit testInit;

    @BeforeEach
    void setUp() {
        testInit = new TestInit();
    }

    @Test
    @DisplayName("Asignación simple con constante")
    void testSimpleAssignment() {
        String code = """
            var a = 5;
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar que contiene las instrucciones básicas
        assertTrue(mips.contains(".text"), "Debe tener segmento .text");
        assertTrue(mips.contains("li"), "Debe tener instrucción li (load immediate)");
    }

    @Test
    @DisplayName("Suma simple")
    void testSimpleAddition() {
        String code = """
            var a = 5;
            var b = 3;
            var c = a + b;
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar instrucciones esperadas
        assertTrue(mips.contains("li"), "Debe cargar constantes");
        assertTrue(mips.contains("add"), "Debe tener instrucción add");
    }

    @Test
    @DisplayName("Todas las operaciones aritméticas")
    void testArithmeticOperations() {
        String code = """
            var a = 10;
            var b = 5;
            var c = a + b;
            var d = a - b;
            var e = a * b;
            var f = a / b;
            """;

        String mips = testInit.generateMIPS(code);

        assertTrue(mips.contains("add"), "Debe tener suma");
        assertTrue(mips.contains("sub"), "Debe tener resta");
        assertTrue(mips.contains("mul"), "Debe tener multiplicación");
        assertTrue(mips.contains("div"), "Debe tener división");
    }

    @Test
    @DisplayName("Comparación simple")
    void testSimpleComparison() {
        String code = """
            var a = 10;
            var b = 20;
            if (a < b) {
                var c = 1;
            }
            """;

        String mips = testInit.generateMIPS(code);

        // Debe contener instrucciones de branch o comparación
        boolean hasBranch = mips.contains("beq") || mips.contains("bne") ||
                mips.contains("blt") || mips.contains("bgt") ||
                mips.contains("slt");
        assertTrue(hasBranch, "Debe tener instrucciones de comparación/branch");
    }
}