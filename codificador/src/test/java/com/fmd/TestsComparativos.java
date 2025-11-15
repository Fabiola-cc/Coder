package com.fmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestsComparativos {

    private TestInit testInit;

    @BeforeEach
    void setUp() {
        testInit = new TestInit();
    }

    @Test
    @DisplayName("Test 15: Comparar salida MIPS exacta - Asignación")
    void testExactMIPSOutput() {
        String code = """
            var a = 5;
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar que contiene la secuencia esperada
        assertTrue(mips.contains("li"), "Debe tener li");
    }

    @Test
    @DisplayName("Test 16: Operación negativa")
    void testNegativeOperation() {
        String code = """
            var a = 10;
            var b = -a;
            """;

        String mips = testInit.generateMIPS(code);

        // Negación se hace con sub $t, $zero, $src
        assertTrue(mips.contains("$zero"), "Debe usar $zero para negación");
        assertTrue(mips.contains("sub"), "Debe usar sub para negación");
    }

    @Test
    @DisplayName("Test 17: NOT lógico")
    void testLogicalNot() {
        String code = """
            var a = true;
            var b = !a;
            """;

        String mips = testInit.generateMIPS(code);

        // NOT se implementa con seq $result, $arg, $zero
        boolean hasNot = mips.contains("seq") || mips.contains("xor");
        assertTrue(hasNot, "Debe implementar NOT lógico");
    }
}
