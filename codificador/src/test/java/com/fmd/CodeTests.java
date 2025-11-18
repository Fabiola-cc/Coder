package com.fmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CodeTests {
    private TestInit testInit;

    @BeforeEach
    void setUp() {
        testInit = new TestInit();
    }

    @Test
    @DisplayName("Operacion entre variables con syscall sencilla")
    void testPrint() {
        String code = """
            let a: integer = 5;
            let b: integer = 7;
            let c: integer = a + b;
            print(c);
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar que contiene las instrucciones básicas
        assertEquals("""
                .data
                
                
                .text
                .globl main
                main:
                    move    $fp, $sp
                
                    li      $t0, 5
                    move    $t1, $t0
                    li      $t0, 7
                    move    $t2, $t0
                    add     $t0, $t1, $t2
                    move    $t3, $t0
                    move    $t0, $t3
                    move    $a0, $t3
                    li      $v0, 1
                    syscall\s
                
                    # Fin del programa
                    li      $v0, 10
                    syscall
                
                print:
                    addi    $sp, $sp, -4
                    sw      $ra, 0($sp)
                    li      $t9, 268435456
                    bge     $a0, $t9, print_as_string
                print_as_int:
                    li      $v0, 1
                    syscall\s
                    j       print_end
                print_as_string:
                    li      $v0, 4
                    syscall\s
                print_end:
                    lw      $ra, 0($sp)
                    addi    $sp, $sp, 4
                    jr      $ra
                read_int:
                    li      $v0, 5
                    syscall\s
                    jr      $ra
                print_bool:
                    beq     $a0, $zero, print_bool_false
                    la      $a0, true_str
                    li      $v0, 4
                    syscall\s
                    j       print_bool_end
                print_bool_false:
                    la      $a0, false_str
                    li      $v0, 4
                    syscall\s
                print_bool_end:
                    jr      $ra
                print_newline:
                    la      $a0, newline
                    li      $v0, 4
                    syscall\s
                    jr      $ra
                concat_strings:
                    addi    $sp, $sp, -20
                    sw      $ra, 16($sp)
                    sw      $s0, 12($sp)
                    sw      $s1, 8($sp)
                    sw      $s2, 4($sp)
                    sw      $s3, 0($sp)
                    move    $s0, $a0
                    move    $s1, $a1
                    move    $t0, $s0
                    li      $s2, 0
                cs_len1:
                    lb      $t1, 0($t0)
                    beqz    $t1, cs_len1_done
                    addi    $t0, $t0, 1
                    addi    $s2, $s2, 1
                    j       cs_len1
                cs_len1_done:
                    move    $t0, $s1
                    li      $s3, 0
                cs_len2:
                    lb      $t1, 0($t0)
                    beqz    $t1, cs_len2_done
                    addi    $t0, $t0, 1
                    addi    $s3, $s3, 1
                    j       cs_len2
                cs_len2_done:
                    add     $a0, $s2, $s3
                    addi    $a0, $a0, 1
                    li      $v0, 9
                    syscall\s
                    move    $t2, $v0
                    move    $t0, $s0
                    move    $t1, $t2
                cs_copy1:
                    lb      $t3, 0($t0)
                    beqz    $t3, cs_copy1_done
                    sb      $t3, 0($t1)
                    addi    $t0, $t0, 1
                    addi    $t1, $t1, 1
                    j       cs_copy1
                cs_copy1_done:
                    move    $t0, $s1
                cs_copy2:
                    lb      $t3, 0($t0)
                    sb      $t3, 0($t1)
                    beqz    $t3, cs_done
                    addi    $t0, $t0, 1
                    addi    $t1, $t1, 1
                    j       cs_copy2
                cs_done:
                    move    $v0, $t2
                    lw      $s3, 0($sp)
                    lw      $s2, 4($sp)
                    lw      $s1, 8($sp)
                    lw      $s0, 12($sp)
                    lw      $ra, 16($sp)
                    addi    $sp, $sp, 20
                    jr      $ra
                """, mips, "Codigo MIPS correcto");
    }

    @Test
    @DisplayName("Función con retorno")
    void testFunction() {
        String code = """
            function cuadrado(x: integer): integer {
                return x * x;
            }
            let r: integer = cuadrado(6);
            print(r);
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar que contiene las instrucciones básicas
        assertEquals("""
                .data
                
                
                .text
                .globl main
                main:
                    move    $fp, $sp
        
                    li      $t1, 6
                    move    $a0, $t1
                    jal     cuadrado
                    move    $t1, $v0
                    move    $t0, $t1
                    move    $t1, $t0
                    move    $a0, $t0
                    li      $v0, 1
                    syscall\s
        
                    # Fin del programa
                    li      $v0, 10
                    syscall
        
                cuadrado:
                    addi    $sp, $sp, -12
                    sw      $fp, 4($sp)
                    sw      $ra, 8($sp)
                    move    $fp, $sp
                    sw      $a0, 0($fp)
                    mul     $t0, $t1, $t1
                    move    $v0, $t0
                    j       cuadrado_epilog
                cuadrado_epilog:
                    move    $sp, $fp
                    lw      $fp, 4($sp)
                    lw      $ra, 8($sp)
                    addi    $sp, $sp, 12
                    jr      $ra
        
                print:
                    addi    $sp, $sp, -4
                    sw      $ra, 0($sp)
                    li      $t9, 268435456
                    bge     $a0, $t9, print_as_string
                print_as_int:
                    li      $v0, 1
                    syscall\s
                    j       print_end
                print_as_string:
                    li      $v0, 4
                    syscall\s
                print_end:
                    lw      $ra, 0($sp)
                    addi    $sp, $sp, 4
                    jr      $ra
                read_int:
                    li      $v0, 5
                    syscall\s
                    jr      $ra
                print_bool:
                    beq     $a0, $zero, print_bool_false
                    la      $a0, true_str
                    li      $v0, 4
                    syscall\s
                    j       print_bool_end
                print_bool_false:
                    la      $a0, false_str
                    li      $v0, 4
                    syscall\s
                print_bool_end:
                    jr      $ra
                print_newline:
                    la      $a0, newline
                    li      $v0, 4
                    syscall\s
                    jr      $ra
                concat_strings:
                    addi    $sp, $sp, -20
                    sw      $ra, 16($sp)
                    sw      $s0, 12($sp)
                    sw      $s1, 8($sp)
                    sw      $s2, 4($sp)
                    sw      $s3, 0($sp)
                    move    $s0, $a0
                    move    $s1, $a1
                    move    $t0, $s0
                    li      $s2, 0
                cs_len1:
                    lb      $t1, 0($t0)
                    beqz    $t1, cs_len1_done
                    addi    $t0, $t0, 1
                    addi    $s2, $s2, 1
                    j       cs_len1
                cs_len1_done:
                    move    $t0, $s1
                    li      $s3, 0
                cs_len2:
                    lb      $t1, 0($t0)
                    beqz    $t1, cs_len2_done
                    addi    $t0, $t0, 1
                    addi    $s3, $s3, 1
                    j       cs_len2
                cs_len2_done:
                    add     $a0, $s2, $s3
                    addi    $a0, $a0, 1
                    li      $v0, 9
                    syscall\s
                    move    $t2, $v0
                    move    $t0, $s0
                    move    $t1, $t2
                cs_copy1:
                    lb      $t3, 0($t0)
                    beqz    $t3, cs_copy1_done
                    sb      $t3, 0($t1)
                    addi    $t0, $t0, 1
                    addi    $t1, $t1, 1
                    j       cs_copy1
                cs_copy1_done:
                    move    $t0, $s1
                cs_copy2:
                    lb      $t3, 0($t0)
                    sb      $t3, 0($t1)
                    beqz    $t3, cs_done
                    addi    $t0, $t0, 1
                    addi    $t1, $t1, 1
                    j       cs_copy2
                cs_done:
                    move    $v0, $t2
                    lw      $s3, 0($sp)
                    lw      $s2, 4($sp)
                    lw      $s1, 8($sp)
                    lw      $s0, 12($sp)
                    lw      $ra, 16($sp)
                    addi    $sp, $sp, 20
                    jr      $ra
                """, mips, "Codigo MIPS correcto");
    }

    @Test
    @DisplayName("While dentro de If")
    void testIfWhile() {
        String code = """
            let n: integer = 5;
            
            if (n > 0) {
                let i: integer = 1;
                while (i <= n) {
                    print(i);
                    i = i + 1;
                }
            } else {
                print(0);
            }
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar que contiene las instrucciones básicas
        assertEquals("""
               .data
                
                
               .text
               .globl main
               main:
                   move    $fp, $sp
        
                   li      $t0, 5
                   move    $t1, $t0
                   li      $t0, 0
                   slt     $t2, $t0, $t1
                   li      $t3, 0
                   beq     $t2, $t3, L1
                   li      $t0, 1
                   move    $t4, $t0
               L3:
                   slt     $t0, $t1, $t4
                   xori    $t0, $t0, 1
                   li      $t3, 0
                   beq     $t0, $t3, L4
                   move    $t5, $t4
                   move    $a0, $t4
                   jal     print
                   li      $t5, 1
                   add     $t2, $t4, $t5
                   move    $t4, $t2
                   j       L3
               L4:
                   j       L2
               L1:
                   li      $t1, 0
                   li      $a0, 0
                   li      $v0, 1
                   syscall\s
               L2:
        
                   # Fin del programa
                   li      $v0, 10
                   syscall
        
               print:
                   addi    $sp, $sp, -4
                   sw      $ra, 0($sp)
                   li      $t9, 268435456
                   bge     $a0, $t9, print_as_string
               print_as_int:
                   li      $v0, 1
                   syscall\s
                   j       print_end
               print_as_string:
                   li      $v0, 4
                   syscall\s
               print_end:
                   lw      $ra, 0($sp)
                   addi    $sp, $sp, 4
                   jr      $ra
               read_int:
                   li      $v0, 5
                   syscall\s
                   jr      $ra
               print_bool:
                   beq     $a0, $zero, print_bool_false
                   la      $a0, true_str
                   li      $v0, 4
                   syscall\s
                   j       print_bool_end
               print_bool_false:
                   la      $a0, false_str
                   li      $v0, 4
                   syscall\s
               print_bool_end:
                   jr      $ra
               print_newline:
                   la      $a0, newline
                   li      $v0, 4
                   syscall\s
                   jr      $ra
               concat_strings:
                   addi    $sp, $sp, -20
                   sw      $ra, 16($sp)
                   sw      $s0, 12($sp)
                   sw      $s1, 8($sp)
                   sw      $s2, 4($sp)
                   sw      $s3, 0($sp)
                   move    $s0, $a0
                   move    $s1, $a1
                   move    $t0, $s0
                   li      $s2, 0
               cs_len1:
                   lb      $t1, 0($t0)
                   beqz    $t1, cs_len1_done
                   addi    $t0, $t0, 1
                   addi    $s2, $s2, 1
                   j       cs_len1
               cs_len1_done:
                   move    $t0, $s1
                   li      $s3, 0
               cs_len2:
                   lb      $t1, 0($t0)
                   beqz    $t1, cs_len2_done
                   addi    $t0, $t0, 1
                   addi    $s3, $s3, 1
                   j       cs_len2
               cs_len2_done:
                   add     $a0, $s2, $s3
                   addi    $a0, $a0, 1
                   li      $v0, 9
                   syscall\s
                   move    $t2, $v0
                   move    $t0, $s0
                   move    $t1, $t2
               cs_copy1:
                   lb      $t3, 0($t0)
                   beqz    $t3, cs_copy1_done
                   sb      $t3, 0($t1)
                   addi    $t0, $t0, 1
                   addi    $t1, $t1, 1
                   j       cs_copy1
               cs_copy1_done:
                   move    $t0, $s1
               cs_copy2:
                   lb      $t3, 0($t0)
                   sb      $t3, 0($t1)
                   beqz    $t3, cs_done
                   addi    $t0, $t0, 1
                   addi    $t1, $t1, 1
                   j       cs_copy2
               cs_done:
                   move    $v0, $t2
                   lw      $s3, 0($sp)
                   lw      $s2, 4($sp)
                   lw      $s1, 8($sp)
                   lw      $s0, 12($sp)
                   lw      $ra, 16($sp)
                   addi    $sp, $sp, 20
                   jr      $ra
                """, mips, "Codigo MIPS correcto");
    }

    @Test
    @DisplayName("Clase y acceso a propiedad")
    void testClassProperty() {
        String code = """
            class Persona {
                let nombre: string;
                let edad: integer;
            
                function constructor(nombre: string, edad: integer) {
                    this.nombre = nombre;
                    this.edad = edad;
                }
            
                function saludar(): string {
                    return "Hola " + this.nombre;
                }
            }
            
            let p: Persona = new Persona("Juan", 25);
            print(p.edad);
            """;

        String mips = testInit.generateMIPS(code);

        // Verificar que contiene las instrucciones básicas
        assertEquals("""
                .data
                str_0: .asciiz "Hola "
                str_1: .asciiz "Juan"
                
                
                .text
                .globl main
                main:
                    move    $fp, $sp
                
                    la      $t1, str_1
                    move    $t0, $t1
                    li      $t2, 25
                    move    $t3, $t2
                    # new Persona
                    li      $a0, 12
                    li      $v0, 9
                    syscall\s
                    move    $s1, $v0
                    move    $a0, $s1
                    move    $a1, $t0
                    move    $a2, $t3
                    jal     Persona_constructor
                    # Property get: t3 = p.edad
                    lw      $t3, 8($s1)
                    move    $a0, $t2
                    jal     print
                
                    # Fin del programa
                    li      $v0, 10
                    syscall
                
                    # Class Persona
                    # end Class Persona
                
                Persona_saludar:
                    addi    $sp, $sp, -12
                    sw      $fp, 4($sp)
                    sw      $ra, 8($sp)
                    move    $fp, $sp
                    sw      $a0, 0($fp)
                    la      $t0, str_0
                    # Property get: t2 = this.nombre
                    lw      $t1, 0($a0)
                    # String concat: t3 = t1 + t2
                    move    $a0, $t0
                    move    $a1, $t1
                    jal     concat_strings
                    move    $t1, $v0
                    move    $v0, $t1
                    j       saludar_epilog
                saludar_epilog:
                    move    $sp, $fp
                    lw      $fp, 4($sp)
                    lw      $ra, 8($sp)
                    addi    $sp, $sp, 12
                    jr      $ra
                
                Persona_constructor:
                    addi    $sp, $sp, -28
                    sw      $fp, 20($sp)
                    sw      $ra, 24($sp)
                    move    $fp, $sp
                    sw      $a0, 0($fp)
                    sw      $a1, 4($fp)
                    sw      $a2, 8($fp)
                    # Property set: this.nombre = nombre
                    lw      $t8, 0($fp)
                    sw      $t0, 0($t8)
                    # Property set: this.edad = edad
                    lw      $t8, 0($fp)
                    sw      $t1, 8($t8)
                constructor_epilog:
                    move    $sp, $fp
                    lw      $fp, 20($sp)
                    lw      $ra, 24($sp)
                    addi    $sp, $sp, 28
                    jr      $ra
                
                print:
                    addi    $sp, $sp, -4
                    sw      $ra, 0($sp)
                    li      $t9, 268435456
                    bge     $a0, $t9, print_as_string
                print_as_int:
                    li      $v0, 1
                    syscall\s
                    j       print_end
                print_as_string:
                    li      $v0, 4
                    syscall\s
                print_end:
                    lw      $ra, 0($sp)
                    addi    $sp, $sp, 4
                    jr      $ra
                read_int:
                    li      $v0, 5
                    syscall\s
                    jr      $ra
                print_bool:
                    beq     $a0, $zero, print_bool_false
                    la      $a0, true_str
                    li      $v0, 4
                    syscall\s
                    j       print_bool_end
                print_bool_false:
                    la      $a0, false_str
                    li      $v0, 4
                    syscall\s
                print_bool_end:
                    jr      $ra
                print_newline:
                    la      $a0, newline
                    li      $v0, 4
                    syscall\s
                    jr      $ra
                concat_strings:
                    addi    $sp, $sp, -20
                    sw      $ra, 16($sp)
                    sw      $s0, 12($sp)
                    sw      $s1, 8($sp)
                    sw      $s2, 4($sp)
                    sw      $s3, 0($sp)
                    move    $s0, $a0
                    move    $s1, $a1
                    move    $t0, $s0
                    li      $s2, 0
                cs_len1:
                    lb      $t1, 0($t0)
                    beqz    $t1, cs_len1_done
                    addi    $t0, $t0, 1
                    addi    $s2, $s2, 1
                    j       cs_len1
                cs_len1_done:
                    move    $t0, $s1
                    li      $s3, 0
                cs_len2:
                    lb      $t1, 0($t0)
                    beqz    $t1, cs_len2_done
                    addi    $t0, $t0, 1
                    addi    $s3, $s3, 1
                    j       cs_len2
                cs_len2_done:
                    add     $a0, $s2, $s3
                    addi    $a0, $a0, 1
                    li      $v0, 9
                    syscall\s
                    move    $t2, $v0
                    move    $t0, $s0
                    move    $t1, $t2
                cs_copy1:
                    lb      $t3, 0($t0)
                    beqz    $t3, cs_copy1_done
                    sb      $t3, 0($t1)
                    addi    $t0, $t0, 1
                    addi    $t1, $t1, 1
                    j       cs_copy1
                cs_copy1_done:
                    move    $t0, $s1
                cs_copy2:
                    lb      $t3, 0($t0)
                    sb      $t3, 0($t1)
                    beqz    $t3, cs_done
                    addi    $t0, $t0, 1
                    addi    $t1, $t1, 1
                    j       cs_copy2
                cs_done:
                    move    $v0, $t2
                    lw      $s3, 0($sp)
                    lw      $s2, 4($sp)
                    lw      $s1, 8($sp)
                    lw      $s0, 12($sp)
                    lw      $ra, 16($sp)
                    addi    $sp, $sp, 20
                    jr      $ra
                """, mips, "Codigo MIPS correcto");
    }
}
