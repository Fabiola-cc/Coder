package com.fmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestsEstructura {
    private TestInit testInit;

    @BeforeEach
    void setUp() {
        testInit = new TestInit();
    }

    @Test
    @DisplayName("Verificar estructura básica MIPS")
    void testMIPSStructure() {
        String code = """
            var x = 10;
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar orden correcto de secciones
        int dataIndex = mips.indexOf(".data");
        int textIndex = mips.indexOf(".text");
        int mainIndex = mips.indexOf("main");

        assertTrue(dataIndex < textIndex, ".data debe ir antes de .text");
        assertTrue(textIndex < mainIndex, ".text debe ir antes de main");
    }

    @Test
    @DisplayName("Contar instrucciones generadas")
    void testInstructionCount() {
        String code = """
            var a = 5;
            var b = 3;
            var c = a + b;
            """;

        String mips = testInit.generateMIPS(code);
        int count = testInit.countMIPSInstructions(mips);

        assertTrue(count >= 3, "Debe generar al menos 3 instrucciones (2 li + 1 add)");
    }

}
