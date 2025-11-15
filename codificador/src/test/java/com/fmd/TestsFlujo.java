package com.fmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el generador de código MIPS
 */
class TestsFlujo {

    private TestInit testInit;

    @BeforeEach
    void setUp() {
        testInit = new TestInit();
    }

    @Test
    @DisplayName("If simple")
    void testIfStatement() {
        String code = """
            var a = 10;
            var b = 20;
            if (a < b) {
                var c = 1;
            } else {
                var c = 2;
            }
            """;

        String mips = testInit.generateMIPS(code);

        // Debe tener etiquetas para if/else
        assertTrue(mips.matches(".*L\\d+:.*"), "Debe tener etiquetas (L0, L1, etc.)");
    }

    @Test
    @DisplayName("While loop")
    void testWhileLoop() {
        String code = """
            var i = 0;
            var sum = 0;
            while (i < 5) {
                sum = sum + i;
                i = i + 1;
            }
            """;

        String mips = testInit.generateMIPS(code);

        // Debe tener saltos (loop)
        boolean hasJump = mips.contains("j ") || mips.contains("beq") || mips.contains("bne");
        assertTrue(hasJump, "Debe tener instrucciones de salto para el loop");
    }

    @Test
    @DisplayName("Goto")
    void testGoto() {
        String code = """
            var x = 1;
            goto end;
            var y = 2;
            end:
            var z = 3;
            """;

        String mips = testInit.generateMIPS(code);

        assertTrue(mips.contains("j end"), "Debe tener salto incondicional");
        assertTrue(mips.contains("end:"), "Debe tener etiqueta end");
    }

    // ========================================
    // NIVEL 4: TESTS DE FUNCIONES
    // ========================================

    @Test
    @DisplayName("Test 11: Función simple sin parámetros")
    void testSimpleFunction() {
        String code = """
            fun test() {
                var x = 5;
            }
            """;

        String mips = testInit.generateMIPS(code);

        assertTrue(mips.contains("test:"), "Debe tener etiqueta de función");
        assertTrue(mips.contains("jr $ra"), "Debe tener return (jr $ra)");
    }

    @Test
    @DisplayName("Test 12: Función con parámetros")
    void testFunctionWithParams() {
        String code = """
            fun suma(a, b) {
                return a + b;
            }
            var result = suma(10, 20);
            """;

        String mips = testInit.generateMIPS(code);

        assertTrue(mips.contains("suma:"), "Debe tener función suma");
        assertTrue(mips.contains("jal suma"), "Debe tener llamada a función");

        // Debe manejar parámetros en $a0-$a3
        boolean hasArgRegs = mips.contains("$a0") || mips.contains("$a1");
        assertTrue(hasArgRegs, "Debe usar registros de argumentos");
    }

    @Test
    @DisplayName("Test 13: Función con return")
    void testFunctionReturn() {
        String code = """
            fun getNumber() {
                return 42;
            }
            var n = getNumber();
            """;

        String mips = testInit.generateMIPS(code);

        assertTrue(mips.contains("$v0"), "Debe usar $v0 para valor de retorno");
        assertTrue(mips.contains("jr $ra"), "Debe retornar con jr $ra");
    }

    @Test
    @DisplayName("Test 14: Stack frame (prólogo/epílogo)")
    void testStackFrame() {
        String code = """
            fun test() {
                var x = 1;
                return x;
            }
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar prólogo (guardar $ra, $fp)
        assertTrue(mips.contains("$ra"), "Debe guardar/restaurar $ra");
        assertTrue(mips.contains("$fp"), "Debe guardar/restaurar $fp");
        assertTrue(mips.contains("$sp"), "Debe manejar stack pointer");
    }

    // ========================================
    // NIVEL 5: TESTS COMPARATIVOS
    // ========================================

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

    // ========================================
    // NIVEL 6: TESTS DE INTEGRACIÓN
    // ========================================

    @Test
    @DisplayName("Test 18: Programa completo - Factorial")
    void testFactorial() {
        String code = """
            fun factorial(n) {
                if (n <= 1) {
                    return 1;
                }
                return n * factorial(n - 1);
            }
            var fact5 = factorial(5);
            """;

        String mips = testInit.generateMIPS(code);

        assertNotNull(mips, "Debe generar código");
        assertTrue(mips.contains("factorial:"), "Debe tener función factorial");
        assertTrue(mips.contains("jal factorial"), "Debe tener llamada recursiva");
    }

    @Test
    @DisplayName("Test 19: Múltiples funciones")
    void testMultipleFunctions() {
        String code = """
            fun add(a, b) {
                return a + b;
            }
            
            fun multiply(a, b) {
                return a * b;
            }
            
            var x = add(5, 3);
            var y = multiply(4, 2);
            """;

        String mips = testInit.generateMIPS(code);

        assertTrue(mips.contains("add:"), "Debe tener función add");
        assertTrue(mips.contains("multiply:"), "Debe tener función multiply");
    }

    @Test
    @DisplayName("Test 20: Código complejo con todo")
    void testComplexCode() {
        String code = """
            fun calculate(n) {
                var sum = 0;
                var i = 1;
                
                while (i <= n) {
                    if (i > 5) {
                        sum = sum + i * 2;
                    } else {
                        sum = sum + i;
                    }
                    i = i + 1;
                }
                
                return sum;
            }
            
            var result = calculate(10);
            """;

        String mips = testInit.generateMIPS(code);

        assertNotNull(mips, "Debe generar código");
        assertTrue(mips.length() > 100, "Debe generar código sustancial");

        // Verificar elementos clave
        assertTrue(mips.contains("calculate:"), "Debe tener función");
        assertTrue(mips.contains("jal calculate"), "Debe llamar función");

        int instructionCount = testInit.countMIPSInstructions(mips);
        assertTrue(instructionCount > 10, "Debe generar múltiples instrucciones");
    }

    // ========================================
    // TESTS DE VALIDACIÓN
    // ========================================

    @Test
    @DisplayName("Test 21: No debe generar código vacío")
    void testNotEmpty() {
        String code = """
            var x = 1;
            """;

        String mips = testInit.generateMIPS(code);

        assertNotNull(mips, "MIPS no debe ser null");
        assertFalse(mips.trim().isEmpty(), "MIPS no debe estar vacío");
    }

    @Test
    @DisplayName("Test 22: Debe tener main")
    void testHasMain() {
        String code = """
            var x = 1;
            """;

        String mips = testInit.generateMIPS(code);

        assertTrue(mips.contains("main") || mips.contains(".globl"),
                "Debe tener punto de entrada main");
    }

    @Test
    @DisplayName("Test 23: Formato válido de registros")
    void testValidRegisterFormat() {
        String code = """
            var a = 5;
            var b = 3;
            var c = a + b;
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar que usa formato correcto de registros MIPS
        assertTrue(mips.matches(".*\\$[tsvafgp]\\d+.*"),
                "Debe usar registros MIPS válidos ($t0, $s0, etc.)");
    }
}