package com.fmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests exhaustivos para verificar que los syscalls están implementados correctamente
 */
class SyscallTests {

    private TestInit testInit;

    @BeforeEach
    void setUp() {
        testInit = new TestInit();
    }

    // AGREGAR después de generateMIPS:
    private String generateAndPrint(String code) {
        String mips = testInit.generateMIPS(code);
        System.out.println("=== MIPS GENERADO ===");
        System.out.println(mips);
        System.out.println("====================\n");
        return mips;
    }

    // ============================================
    // TESTS BÁSICOS - Verificar existencia
    // ============================================

    @Test
    @DisplayName("Syscall Test 1: Función print existe")
    void testPrintFunctionExists() {
        String code = """
            let x: integer = 5;
            print(x);
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("print:"),
                "Debe existir la función print");
        assertTrue(mips.contains("syscall"),
                "Debe contener instrucciones syscall");
        assertTrue(mips.contains("jal") && (mips.contains("print") || mips.contains("syscall")),
                "Debe llamar a la función print o hacer syscall directo");
    }

    @Test
    @DisplayName("Syscall Test 2: Función read_int existe")
    void testReadIntFunctionExists() {
        String code = """
            let x: integer = read_int();
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("read_int:"),
                "Debe existir la función read_int");
        assertTrue(mips.contains("li") && mips.contains("$v0") && mips.contains("5"),
                "Debe tener li $v0, 5 (syscall read_int)");
    }

    @Test
    @DisplayName("Syscall Test 4: Función print_newline existe")
    void testPrintNewlineFunctionExists() {
        String code = """
            let x: integer = 1;
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("print_newline:"),
                "Debe existir la función print_newline");
        assertTrue(mips.contains("newline"),
                "Debe tener label 'newline' en .data");
    }

    @Test
    @DisplayName("Syscall Test 5: Función print_bool existe")
    void testPrintBoolFunctionExists() {
        String code = """
            let flag: boolean = true;
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("print_bool:"),
                "Debe existir la función print_bool");
        assertTrue(mips.contains("true_str") && mips.contains("false_str"),
                "Debe tener strings para true y false en .data");
    }

    // ============================================
    // TESTS DE TIPOS - Verificar detección correcta
    // ============================================

    @Test
    @DisplayName("Syscall Test 6: Print de integer usa syscall 1")
    void testPrintInteger() {
        String code = """
            let num: integer = 42;
            print(num);
            """;

        String mips = generateAndPrint(code);

        boolean hasPrintInt = mips.contains("li") &&
                mips.contains("$v0") &&
                mips.contains("1");

        assertTrue(hasPrintInt, "Debe usar syscall 1 para imprimir enteros");
    }

    @Test
    @DisplayName("Syscall Test 7: Print de string usa syscall 4")
    void testPrintString() {
        String code = """
            let msg: string = "Hello";
            print(msg);
            """;

        String mips = generateAndPrint(code);

        boolean hasPrintString = mips.contains("li") &&
                mips.contains("$v0") &&
                mips.contains("4");

        assertTrue(hasPrintString, "Debe usar syscall 4 para imprimir strings");
    }

    @Test
    @DisplayName("Syscall Test 8: Print de string literal directo")
    void testPrintStringLiteral() {
        String code = """
            print("Direct string");
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("Direct string"),
                "Debe tener el string literal en .data");
        assertTrue(mips.contains("syscall"),
                "Debe usar syscall para imprimir");
    }

    @Test
    @DisplayName("Syscall Test 9: Print de boolean usa print_bool")
    void testPrintBoolean() {
        String code = """
            let flag: boolean = true;
            print(flag);
            """;

        String mips = generateAndPrint(code);

        boolean hasBoolSupport = mips.contains("print_bool") ||
                (mips.contains("true_str") && mips.contains("false_str"));

        assertTrue(hasBoolSupport,
                "Debe tener soporte para imprimir booleanos");
    }

    @Test
    @DisplayName("Syscall Test 10: Print múltiple con diferentes tipos")
    void testPrintMultipleTypes() {
        String code = """
            let x: integer = 10;
            let msg: string = "Hello";
            let flag: boolean = true;
            print(x);
            print(msg);
            print(flag);
            """;

        String mips = generateAndPrint(code);

        int printCalls = countOccurrences(mips, "print");
        assertTrue(printCalls >= 3,
                "Debe tener múltiples referencias a print");
    }

    // ============================================
    // TESTS DE ESTRUCTURA - Verificar stack correcto
    // ============================================

    @Test
    @DisplayName("Syscall Test 11: Print guarda y restaura $ra")
    void testPrintSavesRA() {
        String code = """
            let x: integer = 5;
            print(x);
            """;

        String mips = generateAndPrint(code);

        int printIndex = mips.indexOf("print:");
        if (printIndex != -1) {
            String printFunction = mips.substring(printIndex,
                    Math.min(printIndex + 500, mips.length()));

            assertTrue(printFunction.contains("sw") && printFunction.contains("$ra"),
                    "Print debe guardar $ra en el stack");
            assertTrue(printFunction.contains("lw") && printFunction.contains("$ra"),
                    "Print debe restaurar $ra del stack");
            assertTrue(printFunction.contains("jr") && printFunction.contains("$ra"),
                    "Print debe retornar con jr $ra");
        }
    }

    @Test
    @DisplayName("Syscall Test 12: Print ajusta stack pointer correctamente")
    void testPrintStackManagement() {
        String code = """
            let x: integer = 5;
            print(x);
            """;

        String mips = generateAndPrint(code);

        int printIndex = mips.indexOf("print:");
        if (printIndex != -1) {
            String printFunction = mips.substring(printIndex,
                    Math.min(printIndex + 500, mips.length()));

            assertTrue(printFunction.contains("addi") && printFunction.contains("$sp"),
                    "Print debe ajustar el stack pointer");

            // Verificar que hay tanto decremento como incremento
            int decrementCount = countOccurrences(printFunction, "addi    $sp, $sp, -");
            int incrementCount = countOccurrences(printFunction, "addi    $sp, $sp, ");

            assertTrue(decrementCount > 0 && incrementCount > 0,
                    "Print debe decrementar y restaurar $sp");
        }
    }

    @Test
    @DisplayName("Syscall Test 13: Parámetro en $a0 para print")
    void testParameterInA0ForPrint() {
        String code = """
            let x: integer = 99;
            print(x);
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("$a0"),
                "Debe usar $a0 para pasar parámetros");

        // Verificar que se mueve algo a $a0 antes de print
        boolean hasCorrectPattern = mips.matches("(?s).*move.*\\$a0.*print.*") ||
                mips.matches("(?s).*la.*\\$a0.*print.*") ||
                mips.matches("(?s).*li.*\\$a0.*print.*");

        assertTrue(hasCorrectPattern,
                "Debe mover el parámetro a $a0 antes de llamar print");
    }

    @Test
    @DisplayName("Syscall Test 14: Read_int retorna en $v0")
    void testReadIntReturnsInV0() {
        String code = """
            let x: integer = read_int();
            """;

        String mips = generateAndPrint(code);

        int readIntIndex = mips.indexOf("read_int:");
        if (readIntIndex != -1) {
            String readIntFunction = mips.substring(readIntIndex,
                    Math.min(readIntIndex + 200, mips.length()));

            assertTrue(readIntFunction.contains("li") && readIntFunction.contains("$v0") && readIntFunction.contains("5"),
                    "read_int debe usar syscall 5");
            assertTrue(readIntFunction.contains("jr") && readIntFunction.contains("$ra"),
                    "read_int debe retornar con jr $ra");
        }
    }

    // ============================================
    // TESTS DE DETECCIÓN DE TIPOS
    // ============================================

    @Test
    @DisplayName("Syscall Test 15: Print detecta tipo int vs string por dirección")
    void testPrintTypeDetection() {
        String code = """
            let num: integer = 42;
            print(num);
            """;

        String mips = generateAndPrint(code);

        int printIndex = mips.indexOf("print:");
        if (printIndex != -1) {
            String printFunction = mips.substring(printIndex,
                    Math.min(printIndex + 600, mips.length()));

            // Debe tener lógica de detección (comparación con umbral o branches)
            boolean hasTypeDetection = printFunction.contains("bge") ||
                    printFunction.contains("beq") ||
                    printFunction.contains("print_as_int") ||
                    printFunction.contains("print_as_string");

            assertTrue(hasTypeDetection,
                    "Print debe tener lógica para detectar tipos");
        }
    }

    @Test
    @DisplayName("Syscall Test 16: Print tiene branches para int y string")
    void testPrintHasBranches() {
        String code = """
            let x: integer = 1;
            """;

        String mips = generateAndPrint(code);

        int printIndex = mips.indexOf("print:");
        if (printIndex != -1) {
            String printFunction = mips.substring(printIndex,
                    Math.min(printIndex + 600, mips.length()));

            boolean hasIntBranch = printFunction.contains("print_as_int") ||
                    (printFunction.contains("$v0") && printFunction.contains("1"));
            boolean hasStringBranch = printFunction.contains("print_as_string") ||
                    (printFunction.contains("$v0") && printFunction.contains("4"));

            assertTrue(hasIntBranch && hasStringBranch,
                    "Print debe tener branches separados para int y string");
        }
    }

    // ============================================
    // TESTS DE CASOS ESPECIALES
    // ============================================

    @Test
    @DisplayName("Syscall Test 17: Print con literal numérico")
    void testPrintLiteral() {
        String code = """
            print(123);
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("print") || mips.contains("syscall"),
                "Debe poder imprimir literales");
        assertTrue(mips.contains("123") || mips.contains("li"),
                "Debe cargar el literal");
    }

    @Test
    @DisplayName("Syscall Test 18: Print con expresión aritmética")
    void testPrintExpression() {
        String code = """
            let a: integer = 10;
            let b: integer = 5;
            print(a + b);
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("add"), "Debe tener operación add");
        assertTrue(mips.contains("print") || mips.contains("syscall"),
                "Debe imprimir el resultado");
    }

    @Test
    @DisplayName("Syscall Test 19: Print dentro de loop")
    void testPrintInLoop() {
        String code = """
            for (let i: integer = 0; i < 3; i = i + 1) {
                print(i);
            }
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("print"),
                "Debe tener llamadas a print dentro del loop");
        int jalCount = countOccurrences(mips, "jal");
        assertTrue(jalCount >= 1,
                "Debe tener al menos una llamada jal");
    }

    @Test
    @DisplayName("Syscall Test 20: Print en condición if-else")
    void testPrintInConditional() {
        String code = """
            let x: integer = 5;
            if (x > 3) {
                print(x);
            } else {
                print(0);
            }
            """;

        String mips = generateAndPrint(code);

        int printCount = countOccurrences(mips, "print");
        assertTrue(printCount >= 2,
                "Debe tener prints en ambas ramas del if-else");
    }

    // ============================================
    // TESTS DE SEGMENTO DE DATOS
    // ============================================

    @Test
    @DisplayName("Syscall Test 21: Segmento .data tiene strings necesarios")
    void testDataSegmentHasRequiredStrings() {
        String code = """
            let x: integer = 1;
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains(".data"),
                "Debe tener segmento .data");
        assertTrue(mips.contains("newline"),
                "Debe tener string newline");
        assertTrue(mips.contains("true_str") || mips.contains("false_str"),
                "Debe tener strings para booleanos");
    }

    @Test
    @DisplayName("Syscall Test 22: Strings literales en .data con formato correcto")
    void testStringLiteralsInData() {
        String code = """
            let msg: string = "Hello World";
            print(msg);
            """;

        String mips = generateAndPrint(code);

        int dataIndex = mips.indexOf(".data");
        int textIndex = mips.indexOf(".text");

        assertTrue(dataIndex < textIndex,
                ".data debe aparecer antes de .text");
        assertTrue(mips.contains(".asciiz"),
                "Los strings deben usar directiva .asciiz");
        assertTrue(mips.contains("Hello World"),
                "El string literal debe estar en .data");
    }

    // ============================================
    // TESTS DE INTEGRACIÓN
    // ============================================

    @Test
    @DisplayName("Syscall Test 23: Programa completo con todos los syscalls")
    void testCompleteProgram() {
        String code = """
            let x: integer = 10;
            let msg: string = "Value:";
            let flag: boolean = true;
            
            print(msg);
            print(x);
            print(flag);
            
            let input: integer = read_int();
            print(input);
            """;

        String mips = generateAndPrint(code);

        // Verificar todos los componentes
        assertTrue(mips.contains(".data"), "Debe tener .data");
        assertTrue(mips.contains(".text"), "Debe tener .text");
        assertTrue(mips.contains("main:"), "Debe tener main");
        assertTrue(mips.contains("print:"), "Debe tener función print");
        assertTrue(mips.contains("read_int:"), "Debe tener función read_int");
        assertTrue(mips.contains("syscall"), "Debe usar syscalls");
        assertTrue(mips.contains("li") && mips.contains("$v0") && mips.contains("10"),
                "Debe terminar con exit");
    }

    @Test
    @DisplayName("Syscall Test 24: Todas las funciones runtime están presentes")
    void testAllRuntimeFunctionsPresent() {
        String code = """
            let x: integer = 1;
            """;

        String mips = generateAndPrint(code);

        assertTrue(mips.contains("print:"),
                "Debe tener función print");
        assertTrue(mips.contains("read_int:"),
                "Debe tener función read_int");
        assertTrue(mips.contains("print_newline:"),
                "Debe tener función print_newline");
        assertTrue(mips.contains("print_bool:"),
                "Debe tener función print_bool");
    }


    @Test
    @DisplayName("Syscall Test 26: No hay print undefined")
    void testNoPrintUndefined() {
        String code = """
            let x: integer = 5;
            print(x);
            """;

        String mips = generateAndPrint(code);

        int jalPrintCount = countOccurrences(mips, "jal");
        int printDefCount = countOccurrences(mips, "print:");

        if (jalPrintCount > 0) {
            assertTrue(printDefCount > 0,
                    "Si hay llamadas a funciones, deben estar definidas");
        }
    }

    @Test
    @DisplayName("Syscall Test 27: Syscall numbers correctos")
    void testCorrectSyscallNumbers() {
        String code = """
            let x: integer = 42;
            let msg: string = "Hi";
            print(x);
            print(msg);
            let input: integer = read_int();
            """;
        String mips = generateAndPrint(code);


        // Verificar syscalls individuales
        assertTrue(mips.contains("$v0") && mips.contains("1"),
                "Debe tener syscall 1 (print_int)");
        assertTrue(mips.contains("$v0") && mips.contains("4"),
                "Debe tener syscall 4 (print_string)");
        assertTrue(mips.contains("$v0") && mips.contains("5"),
                "Debe tener syscall 5 (read_int)");
        assertTrue(mips.contains("$v0") && mips.contains("10"),
                "Debe tener syscall 10 (exit)");
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }

        return count;
    }

    private boolean isFunctionWellFormed(String mips, String functionName) {
        int funcIndex = mips.indexOf(functionName + ":");
        if (funcIndex == -1) return false;

        String afterFunc = mips.substring(funcIndex);
        int nextLabelIndex = afterFunc.indexOf("\n", afterFunc.indexOf(":") + 1);
        nextLabelIndex = afterFunc.indexOf(":", nextLabelIndex);

        String functionBody = nextLabelIndex == -1 ?
                afterFunc : afterFunc.substring(0, nextLabelIndex);

        return functionBody.contains("jr $ra") || functionBody.contains("jr  $ra");
    }
}