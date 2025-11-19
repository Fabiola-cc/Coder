.data
newline: .asciiz "\n"
space: .asciiz " "
true_str: .asciiz "true"
false_str: .asciiz "false"
concat_buffer: .space 512
int_buffer: .space 32
str_0: .asciiz "=== 1. Declaraciones y Operaciones ===\n"
str_1: .asciiz "a + b = "
str_2: .asciiz "\n"
str_3: .asciiz "a * b = "
str_4: .asciiz "\n\n"
str_5: .asciiz "=== 2. Funciones ===\n"
str_6: .asciiz "cuadrado(6) = "
str_7: .asciiz "doble(5) = "
str_8: .asciiz "=== 3. Clases y Metodos ===\n"
str_9: .asciiz "3 + 4 = "
str_10: .asciiz "=== 4. Clase con Constructor ===\n"
str_11: .asciiz "Hola "
str_12: .asciiz "Juan"
str_13: .asciiz "Edad de Juan: "
str_14: .asciiz "=== 5. Ciclo FOR ===\n"
str_15: .asciiz "Loop index: "
str_16: .asciiz "=== 6. IF + WHILE ===\n"
str_17: .asciiz "=== 7. FOREACH ===\n"
str_18: .asciiz " "
str_19: .asciiz "=== 8. Arrays y Matrices ===\n"
str_20: .asciiz "arr[2] = "
str_21: .asciiz "arr[0] despues de asignar 99 = "
str_22: .asciiz "=== 9. SWITCH ===\n"
str_23: .asciiz "Uno\n"
str_24: .asciiz "Dos\n"
str_25: .asciiz "Otro\n"
str_26: .asciiz "=== 10. Break y Continue ===\n"
str_27: .asciiz "=== 11. Operadores Logicos ===\n"
str_28: .asciiz "AND test: OK\n"
str_29: .asciiz "AND test: NOP\n"
str_30: .asciiz "OR test: OK\n"
str_31: .asciiz "OR test: NOP\n"
str_32: .asciiz "=== 12. Operador Ternario ===\n"
str_33: .asciiz "(8 > 5) ? 100 : 0 = "
str_34: .asciiz "=== 13. Funcion con IF y WHILE ===\n"
str_35: .asciiz "Valor inicial: "
str_36: .asciiz "Mayor que 5\n"
str_37: .asciiz "5 o menos\n"
str_38: .asciiz "=== 14. Recursividad - Factorial ===\n"
str_39: .asciiz "factorial(5) = "
str_40: .asciiz "=== 15. Recursividad - Fibonacci ===\n"
str_41: .asciiz "fibonacci(10) = "
str_42: .asciiz "=== SUITE DE EJEMPLOS COMPLETADA ===\n"
.align 2
nums: .space 16
.align 2
arr: .space 16


.text
.globl main
main:
    move    $fp, $sp

    # Property get: t1 = "=== 1. Declaraciones y Operaciones ===\n"
    # ERROR: Object symbol not found: "=== 1
    la      $a0, str_0
    li      $v0, 4
    syscall 
    li      $t1, 5
    move    $t2, $t1
    li      $t1, 7
    move    $t3, $t1
    add     $t1, $t2, $t3
    move    $t4, $t1
    la      $t1, str_1
    la      $a0, str_1
    li      $v0, 4
    syscall 
    move    $t1, $t4
    move    $a0, $t4
    li      $v0, 1
    syscall 
    la      $t1, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    mul     $t1, $t2, $t3
    move    $t5, $t1
    la      $t1, str_3
    la      $a0, str_3
    li      $v0, 4
    syscall 
    move    $t1, $t5
    move    $a0, $t5
    li      $v0, 1
    syscall 
    la      $t1, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    # Property get: t1 = "=== 2. Funciones ===\n"
    # ERROR: Object symbol not found: "=== 2
    la      $a0, str_5
    li      $v0, 4
    syscall 
    li      $t1, 6
    move    $a0, $t1
    jal     cuadrado
    move    $s1, $v0
    la      $t1, str_6
    la      $a0, str_6
    li      $v0, 4
    syscall 
    move    $a0, $s1
    li      $v0, 1
    syscall 
    la      $s1, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    li      $t0, 5
    move    $a0, $t0
    jal     doble
    move    $s2, $v0
    la      $t1, str_7
    la      $a0, str_7
    li      $v0, 4
    syscall 
    move    $a0, $s2
    li      $v0, 1
    syscall 
    la      $s2, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    # Property get: t2 = "=== 3. Clases y Metodos ===\n"
    # ERROR: Object symbol not found: "=== 3
    la      $a0, str_8
    li      $v0, 4
    syscall 
    # new MathOps
    li      $a0, 8
    li      $v0, 9
    syscall 
    move    $s1, $v0
    li      $s2, 3
    li      $t0, 4
    # Method call with return: t2 = m.suma
    move    $a0, $s1
    move    $a1, $s2
    move    $a2, $t0
    jal     MathOps_suma
    move    $t0, $v0
    move    $t1, $t0
    la      $t0, str_9
    la      $a0, str_9
    li      $v0, 4
    syscall 
    move    $t0, $t1
    move    $a0, $t1
    li      $v0, 1
    syscall 
    la      $t0, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    # Property get: t2 = "=== 4. Clase con Constructor ===\n"
    # ERROR: Object symbol not found: "=== 4
    la      $a0, str_10
    li      $v0, 4
    syscall 
    la      $t2, str_12
    move    $t1, $t2
    li      $t0, 25
    move    $t3, $t0
    # new Persona
    li      $a0, 12
    li      $v0, 9
    syscall 
    move    $s1, $v0
    move    $a0, $s1
    move    $a1, $t1
    move    $a2, $t3
    jal     Persona_constructor
    la      $t3, str_13
    la      $a0, str_13
    li      $v0, 4
    syscall 
    # Property get: t1 = p.edad
    lw      $t3, 8($s1)
    move    $a0, $t2
    jal     print
    la      $t3, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    # Property get: t1 = "=== 5. Ciclo FOR ===\n"
    # ERROR: Object symbol not found: "=== 5
    la      $a0, str_14
    li      $v0, 4
    syscall 
    li      $t3, 0
    move    $t1, $t3
L1:
    li      $t3, 3
    slt     $t0, $t1, $t3
    li      $t4, 0
    beq     $t0, $t4, L2
    la      $t3, str_15
    la      $a0, str_15
    li      $v0, 4
    syscall 
    move    $a0, $t1
    jal     print
    la      $t3, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    li      $t3, 1
    add     $t0, $t1, $t3
    move    $t1, $t0
    j       L1
L2:
    la      $t5, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    # Property get: t6 = "=== 6. IF + WHILE ===\n"
    # ERROR: Object symbol not found: "=== 6
    la      $a0, str_16
    li      $v0, 4
    syscall 
    li      $t5, 5
    move    $t7, $t5
    li      $t5, 0
    slt     $t0, $t5, $t7
    li      $t4, 0
    beq     $t0, $t4, L3
    li      $t5, 1
    move    $t1, $t5
L5:
    slt     $t0, $t7, $t1
    xori    $t5, $t0, 1
    li      $t4, 0
    beq     $t5, $t4, L6
    move    $t3, $t1
    move    $a0, $t1
    jal     print
    la      $t7, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    li      $t7, 1
    add     $t5, $t1, $t7
    move    $t1, $t5
    j       L5
L6:
    j       L4
L3:
    li      $t3, 0
    li      $a0, 0
    li      $v0, 1
    syscall 
L4:
    la      $t0, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    # Property get: t1 = "=== 7. FOREACH ===\n"
    # ERROR: Object symbol not found: "=== 7
    la      $a0, str_17
    li      $v0, 4
    syscall 
    li      $t0, 1
    sw      $t0, nums+0
    li      $t0, 2
    sw      $t0, nums+4
    li      $t0, 3
    sw      $t0, nums+8
    li      $t0, 0
    li      $t3, 3
L7:
    slt     $t5, $t0, $t3
    li      $t4, 0
    beq     $t5, $t4, L8
    sll     $t8, $t0, 2
    la      $t9, nums
    add     $t8, $t9, $t8
    lw      $t7, 0($t8)
    move    $t0, $t7
    move    $a0, $t0
    jal     print
    la      $t7, str_18
    la      $a0, str_18
    li      $v0, 4
    syscall 
    addi    $t7, $t5, 1
    move    $t5, $t7
    j       L7
L8:
    la      $t7, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    # Property get: t9 = "=== 8. Arrays y Matrices ===\n"
    # ERROR: Object symbol not found: "=== 8
    la      $a0, str_19
    li      $v0, 4
    syscall 
    li      $t7, 10
    sw      $t7, arr+0
    li      $t7, 20
    sw      $t7, arr+4
    li      $t7, 30
    sw      $t7, arr+8
    li      $t7, 2
    move    $t0, $t7
    sll     $t8, $t0, 2
    la      $t9, arr
    add     $t8, $t9, $t8
    lw      $t7, 0($t8)
    move    $t4, $t7
    la      $t7, str_20
    la      $a0, str_20
    li      $v0, 4
    syscall 
    move    $t7, $t4
    move    $a0, $t4
    li      $v0, 1
    syscall 
    la      $t7, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    li      $t7, 99
    sw      $t7, arr+0
    li      $t7, 0
    sll     $t8, $t7, 2
    la      $t9, arr
    add     $t8, $t9, $t8
    lw      $t5, 0($t8)
    la      $t5, str_21
    la      $a0, str_21
    li      $v0, 4
    syscall 
    move    $a0, $t5
    li      $v0, 1
    syscall 
    la      $t5, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    # Property get: t7 = "=== 9. SWITCH ===\n"
    # ERROR: Object symbol not found: "=== 9
    la      $a0, str_22
    li      $v0, 4
    syscall 
    li      $t5, 2
    move    $t7, $t5
    move    $t5, $t7
    li      $t2, 1
    beq     $t5, $t2, L10
    li      $t2, 2
    beq     $t5, $t2, L11
    j       L12
L10:
    la      $t2, str_23
    la      $a0, str_23
    li      $v0, 4
    syscall 
    j       L9
L11:
    la      $t2, str_24
    la      $a0, str_24
    li      $v0, 4
    syscall 
    j       L9
L12:
    la      $t2, str_25
    la      $a0, str_25
    li      $v0, 4
    syscall 
L9:
    la      $t2, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    # Property get: t8 = "=== 10. Break y Continue ===\n"
    # ERROR: Object symbol not found: "=== 10
    la      $a0, str_26
    li      $v0, 4
    syscall 
    li      $t2, 0
L13:
    li      $t2, 5
    slt     $t5, $t2, $t2
    li      $t4, 0
    beq     $t5, $t4, L14
    li      $t2, 1
    add     $t4, $t2, $t2
    move    $t2, $t4
    li      $t4, 2
    sub     $t4, $t2, $t4
    seq     $t2, $t4, $zero
    li      $t4, 0
    beq     $t2, $t4, L15
    j       L13
L15:
    li      $t2, 4
    sub     $t4, $t2, $t2
    seq     $t4, $t4, $zero
    li      $t4, 0
    beq     $t4, $t4, L17
    j       L14
L17:
    move    $t4, $t2
    move    $a0, $t2
    li      $v0, 1
    syscall 
    la      $t4, str_18
    la      $a0, str_18
    li      $v0, 4
    syscall 
    j       L13
L14:
    la      $t5, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    # Property get: t7 = "=== 11. Operadores Logicos ===\n"
    # ERROR: Object symbol not found: "=== 11
    la      $a0, str_27
    li      $v0, 4
    syscall 
    li      $t5, 1
    move    $t4, $t5
    li      $t5, 0
    move    $t6, $t5
    li      $t5, 0
    li      $t4, 0
    beq     $t4, $t4, L19
    seq     $t4, $t6, $zero
    move    $t5, $t4
L19:
    li      $t4, 0
    beq     $t5, $t4, L20
    la      $t4, str_28
    la      $a0, str_28
    li      $v0, 4
    syscall 
    j       L21
L20:
    la      $t4, str_29
    la      $a0, str_29
    li      $v0, 4
    syscall 
L21:
    li      $t5, 0
    move    $t4, $t5
    li      $t5, 1
    move    $t4, $t5
    li      $t5, 1
    li      $t4, 0
    bne     $t4, $t4, L22
    seq     $t4, $t4, $zero
    move    $t5, $t4
L22:
    li      $t4, 0
    beq     $t5, $t4, L23
    la      $t4, str_30
    la      $a0, str_30
    li      $v0, 4
    syscall 
    j       L24
L23:
    la      $t4, str_31
    la      $a0, str_31
    li      $v0, 4
    syscall 
L24:
    la      $t5, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    # Property get: t7 = "=== 12. Operador Ternario ===\n"
    # ERROR: Object symbol not found: "=== 12
    la      $a0, str_32
    li      $v0, 4
    syscall 
    li      $t5, 8
    move    $t4, $t5
    li      $t5, 5
    slt     $t1, $t5, $t4
    li      $t4, 1
    beq     $t1, $t4, L25
    j       L26
L25:
    li      $t2, 100
    move    $t5, $t2
    j       L27
L26:
    li      $t2, 0
    move    $t5, $t2
L27:
    move    $t4, $t5
    la      $t5, str_33
    la      $a0, str_33
    li      $v0, 4
    syscall 
    move    $t5, $t4
    move    $a0, $t4
    li      $v0, 1
    syscall 
    la      $t5, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    # Property get: t7 = "=== 13. Funcion con IF y WHILE ===\n"
    # ERROR: Object symbol not found: "=== 13
    la      $a0, str_34
    li      $v0, 4
    syscall 
    li      $s1, 3
    move    $a0, $s1
    jal     makeAdder
    move    $t1, $v0
    move    $t0, $t1
    la      $t1, str_35
    la      $a0, str_35
    li      $v0, 4
    syscall 
    move    $t1, $t0
    move    $a0, $t0
    li      $v0, 1
    syscall 
    la      $t1, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
L28:
    li      $t1, 7
    slt     $s1, $t0, $t1
    li      $t2, 0
    beq     $s1, $t2, L29
    li      $t1, 5
    slt     $t3, $t1, $t0
    li      $t2, 0
    beq     $t3, $t2, L30
    la      $t1, str_36
    la      $a0, str_36
    li      $v0, 4
    syscall 
    j       L31
L30:
    la      $t1, str_37
    la      $a0, str_37
    li      $v0, 4
    syscall 
L31:
    li      $t3, 1
    add     $t1, $t0, $t3
    move    $t0, $t1
    j       L28
L29:
    la      $s1, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    # Property get: t7 = "=== 14. Recursividad - Factorial ===\n"
    # ERROR: Object symbol not found: "=== 14
    la      $a0, str_38
    li      $v0, 4
    syscall 
    li      $t4, 5
    move    $a0, $t4
    jal     factorial
    move    $t4, $v0
    move    $s1, $t4
    la      $t4, str_39
    la      $a0, str_39
    li      $v0, 4
    syscall 
    move    $a0, $s1
    li      $v0, 1
    syscall 
    la      $s1, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    # Property get: t8 = "=== 15. Recursividad - Fibonacci ===\n"
    # ERROR: Object symbol not found: "=== 15
    la      $a0, str_40
    li      $v0, 4
    syscall 
    li      $s3, 10
    move    $a0, $s3
    jal     fibonacci
    move    $t4, $v0
    move    $s2, $t4
    la      $t4, str_41
    la      $a0, str_41
    li      $v0, 4
    syscall 
    move    $a0, $s2
    li      $v0, 1
    syscall 
    la      $s2, str_4
    la      $a0, str_4
    li      $v0, 4
    syscall 
    la      $s2, str_42
    la      $a0, str_42
    li      $v0, 4
    syscall 

    # Fin del programa
    li      $v0, 10
    syscall

MathOps_suma:
    addi    $sp, $sp, -60
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    sw      $s0, 8($sp)
    sw      $s1, 12($sp)
    sw      $s2, 16($sp)
    sw      $s3, 20($sp)
    sw      $s4, 24($sp)
    sw      $s5, 28($sp)
    sw      $s6, 32($sp)
    sw      $s7, 36($sp)
    # Saved $s0-$s7
    move    $fp, $sp
    sw      $a0, 40($fp)
    # Saved param 0 at 40($fp)
    sw      $a1, 44($fp)
    # Saved param 1 at 44($fp)
    sw      $a2, 48($fp)
    # Saved param 2 at 48($fp)
    # Frame size: 60 (8 overhead + 32 saved regs + 12 params + 8 locals)
    # Loading parameters from frame
    # Skipping 'this' at 40($fp)
    lw      $s0, 44($fp)
    # Loaded param 'a' from 44($fp) into $s0
    lw      $s1, 48($fp)
    # Loaded param 'b' from 48($fp) into $s1
    add     $t0, $s0, $s1
    move    $v0, $t0
    j       MathOps_suma_epilog
MathOps_suma_epilog:
    lw      $s0, 8($fp)
    lw      $s1, 12($fp)
    lw      $s2, 16($fp)
    lw      $s3, 20($fp)
    lw      $s4, 24($fp)
    lw      $s5, 28($fp)
    lw      $s6, 32($fp)
    lw      $s7, 36($fp)
    # Restored $s0-$s7
    lw      $t9, 0($fp)
    lw      $ra, 4($fp)
    addi    $sp, $fp, 60
    move    $fp, $t9
    jr      $ra

fibonacci:
    addi    $sp, $sp, -56
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    sw      $s0, 8($sp)
    sw      $s1, 12($sp)
    sw      $s2, 16($sp)
    sw      $s3, 20($sp)
    sw      $s4, 24($sp)
    sw      $s5, 28($sp)
    sw      $s6, 32($sp)
    sw      $s7, 36($sp)
    # Saved $s0-$s7
    move    $fp, $sp
    sw      $a0, 40($fp)
    # Saved param 0 at 40($fp)
    # Frame size: 56 (8 overhead + 32 saved regs + 4 params + 12 locals)
    # Loading parameters from frame
    lw      $s0, 40($fp)
    # Loaded param 'n' from 40($fp) into $s0
    li      $t0, 1
    slt     $t2, $t0, $s0
    xori    $t1, $t2, 1
    li      $t3, 0
    beq     $t1, $t3, L34
    move    $v0, $s0
    j       fibonacci_epilog
L34:
    li      $t0, 1
    sub     $t4, $s0, $t0
    move    $a0, $t4
    jal     fibonacci
    move    $s2, $v0
    li      $t4, 2
    sub     $t1, $s0, $t4
    move    $a0, $t1
    jal     fibonacci
    move    $s3, $v0
    add     $t4, $s2, $s3
    move    $v0, $t4
    j       fibonacci_epilog
fibonacci_epilog:
    lw      $s0, 8($fp)
    lw      $s1, 12($fp)
    lw      $s2, 16($fp)
    lw      $s3, 20($fp)
    lw      $s4, 24($fp)
    lw      $s5, 28($fp)
    lw      $s6, 32($fp)
    lw      $s7, 36($fp)
    # Restored $s0-$s7
    lw      $t9, 0($fp)
    lw      $ra, 4($fp)
    addi    $sp, $fp, 56
    move    $fp, $t9
    jr      $ra

    # Class MathOps
    # end Class MathOps

    # Class Persona
    # end Class Persona

doble:
    addi    $sp, $sp, -44
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    sw      $s0, 8($sp)
    sw      $s1, 12($sp)
    sw      $s2, 16($sp)
    sw      $s3, 20($sp)
    sw      $s4, 24($sp)
    sw      $s5, 28($sp)
    sw      $s6, 32($sp)
    sw      $s7, 36($sp)
    # Saved $s0-$s7
    move    $fp, $sp
    sw      $a0, 40($fp)
    # Saved param 0 at 40($fp)
    # Frame size: 44 (8 overhead + 32 saved regs + 4 params + 0 locals)
    # Loading parameters from frame
    lw      $s0, 40($fp)
    # Loaded param 'n' from 40($fp) into $s0
    li      $t0, 2
    mul     $t1, $s0, $t0
    move    $v0, $t1
    j       doble_epilog
doble_epilog:
    lw      $s0, 8($fp)
    lw      $s1, 12($fp)
    lw      $s2, 16($fp)
    lw      $s3, 20($fp)
    lw      $s4, 24($fp)
    lw      $s5, 28($fp)
    lw      $s6, 32($fp)
    lw      $s7, 36($fp)
    # Restored $s0-$s7
    lw      $t9, 0($fp)
    lw      $ra, 4($fp)
    addi    $sp, $fp, 44
    move    $fp, $t9
    jr      $ra

Persona_saludar:
    addi    $sp, $sp, -44
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    sw      $s0, 8($sp)
    sw      $s1, 12($sp)
    sw      $s2, 16($sp)
    sw      $s3, 20($sp)
    sw      $s4, 24($sp)
    sw      $s5, 28($sp)
    sw      $s6, 32($sp)
    sw      $s7, 36($sp)
    # Saved $s0-$s7
    move    $fp, $sp
    sw      $a0, 40($fp)
    # Saved param 0 at 40($fp)
    # Frame size: 44 (8 overhead + 32 saved regs + 4 params + 0 locals)
    # Loading parameters from frame
    # Skipping 'this' at 40($fp)
    la      $t0, str_11
    # Property get: t3 = this.nombre
    lw      $t1, 0($a0)
    move    $v0, $t2
    j       Persona_saludar_epilog
Persona_saludar_epilog:
    lw      $s0, 8($fp)
    lw      $s1, 12($fp)
    lw      $s2, 16($fp)
    lw      $s3, 20($fp)
    lw      $s4, 24($fp)
    lw      $s5, 28($fp)
    lw      $s6, 32($fp)
    lw      $s7, 36($fp)
    # Restored $s0-$s7
    lw      $t9, 0($fp)
    lw      $ra, 4($fp)
    addi    $sp, $fp, 44
    move    $fp, $t9
    jr      $ra

cuadrado:
    addi    $sp, $sp, -44
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    sw      $s0, 8($sp)
    sw      $s1, 12($sp)
    sw      $s2, 16($sp)
    sw      $s3, 20($sp)
    sw      $s4, 24($sp)
    sw      $s5, 28($sp)
    sw      $s6, 32($sp)
    sw      $s7, 36($sp)
    # Saved $s0-$s7
    move    $fp, $sp
    sw      $a0, 40($fp)
    # Saved param 0 at 40($fp)
    # Frame size: 44 (8 overhead + 32 saved regs + 4 params + 0 locals)
    # Loading parameters from frame
    lw      $s0, 40($fp)
    # Loaded param 'x' from 40($fp) into $s0
    mul     $t0, $s0, $s0
    move    $v0, $t0
    j       cuadrado_epilog
cuadrado_epilog:
    lw      $s0, 8($fp)
    lw      $s1, 12($fp)
    lw      $s2, 16($fp)
    lw      $s3, 20($fp)
    lw      $s4, 24($fp)
    lw      $s5, 28($fp)
    lw      $s6, 32($fp)
    lw      $s7, 36($fp)
    # Restored $s0-$s7
    lw      $t9, 0($fp)
    lw      $ra, 4($fp)
    addi    $sp, $fp, 44
    move    $fp, $t9
    jr      $ra

factorial:
    addi    $sp, $sp, -44
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    sw      $s0, 8($sp)
    sw      $s1, 12($sp)
    sw      $s2, 16($sp)
    sw      $s3, 20($sp)
    sw      $s4, 24($sp)
    sw      $s5, 28($sp)
    sw      $s6, 32($sp)
    sw      $s7, 36($sp)
    # Saved $s0-$s7
    move    $fp, $sp
    sw      $a0, 40($fp)
    # Saved param 0 at 40($fp)
    # Frame size: 44 (8 overhead + 32 saved regs + 4 params + 0 locals)
    # Loading parameters from frame
    lw      $s0, 40($fp)
    # Loaded param 'n' from 40($fp) into $s0
    li      $t0, 1
    slt     $t2, $t0, $s0
    xori    $t1, $t2, 1
    li      $t3, 0
    beq     $t1, $t3, L32
    li      $t0, 1
    move    $v0, $t0
    j       factorial_epilog
L32:
    li      $t0, 1
    sub     $t4, $s0, $t0
    move    $a0, $t4
    jal     factorial
    move    $t4, $v0
    mul     $t1, $s0, $t4
    move    $v0, $t1
    j       factorial_epilog
factorial_epilog:
    lw      $s0, 8($fp)
    lw      $s1, 12($fp)
    lw      $s2, 16($fp)
    lw      $s3, 20($fp)
    lw      $s4, 24($fp)
    lw      $s5, 28($fp)
    lw      $s6, 32($fp)
    lw      $s7, 36($fp)
    # Restored $s0-$s7
    lw      $t9, 0($fp)
    lw      $ra, 4($fp)
    addi    $sp, $fp, 44
    move    $fp, $t9
    jr      $ra

Persona_constructor:
    addi    $sp, $sp, -60
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    sw      $s0, 8($sp)
    sw      $s1, 12($sp)
    sw      $s2, 16($sp)
    sw      $s3, 20($sp)
    sw      $s4, 24($sp)
    sw      $s5, 28($sp)
    sw      $s6, 32($sp)
    sw      $s7, 36($sp)
    # Saved $s0-$s7
    move    $fp, $sp
    sw      $a0, 40($fp)
    # Saved param 0 at 40($fp)
    sw      $a1, 44($fp)
    # Saved param 1 at 44($fp)
    sw      $a2, 48($fp)
    # Saved param 2 at 48($fp)
    # Frame size: 60 (8 overhead + 32 saved regs + 12 params + 8 locals)
    # Loading parameters from frame
    # Skipping 'this' at 40($fp)
    lw      $s0, 44($fp)
    # Loaded param 'nombre' from 44($fp) into $s0
    lw      $s1, 48($fp)
    # Loaded param 'edad' from 48($fp) into $s1
    # Property set: this.nombre = nombre
    lw      $t8, 0($fp)
    sw      $s0, 0($t8)
    # Property set: this.edad = edad
    lw      $t8, 0($fp)
    sw      $s1, 8($t8)
Persona_constructor_epilog:
    lw      $s0, 8($fp)
    lw      $s1, 12($fp)
    lw      $s2, 16($fp)
    lw      $s3, 20($fp)
    lw      $s4, 24($fp)
    lw      $s5, 28($fp)
    lw      $s6, 32($fp)
    lw      $s7, 36($fp)
    # Restored $s0-$s7
    lw      $t9, 0($fp)
    lw      $ra, 4($fp)
    addi    $sp, $fp, 60
    move    $fp, $t9
    jr      $ra

makeAdder:
    addi    $sp, $sp, -44
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    sw      $s0, 8($sp)
    sw      $s1, 12($sp)
    sw      $s2, 16($sp)
    sw      $s3, 20($sp)
    sw      $s4, 24($sp)
    sw      $s5, 28($sp)
    sw      $s6, 32($sp)
    sw      $s7, 36($sp)
    # Saved $s0-$s7
    move    $fp, $sp
    sw      $a0, 40($fp)
    # Saved param 0 at 40($fp)
    # Frame size: 44 (8 overhead + 32 saved regs + 4 params + 0 locals)
    # Loading parameters from frame
    lw      $s0, 40($fp)
    # Loaded param 'x' from 40($fp) into $s0
    li      $t0, 2
    add     $t1, $s0, $t0
    move    $v0, $t1
    j       makeAdder_epilog
makeAdder_epilog:
    lw      $s0, 8($fp)
    lw      $s1, 12($fp)
    lw      $s2, 16($fp)
    lw      $s3, 20($fp)
    lw      $s4, 24($fp)
    lw      $s5, 28($fp)
    lw      $s6, 32($fp)
    lw      $s7, 36($fp)
    # Restored $s0-$s7
    lw      $t9, 0($fp)
    lw      $ra, 4($fp)
    addi    $sp, $fp, 44
    move    $fp, $t9
    jr      $ra

print:
    addi    $sp, $sp, -4
    sw      $ra, 0($sp)
    li      $t9, 268435456
    bge     $a0, $t9, print_as_string
print_as_int:
    li      $v0, 1
    syscall 
    j       print_end
print_as_string:
    li      $v0, 4
    syscall 
print_end:
    lw      $ra, 0($sp)
    addi    $sp, $sp, 4
    jr      $ra
read_int:
    li      $v0, 5
    syscall 
    jr      $ra
print_bool:
    beq     $a0, $zero, print_bool_false
    la      $a0, true_str
    li      $v0, 4
    syscall 
    j       print_bool_end
print_bool_false:
    la      $a0, false_str
    li      $v0, 4
    syscall 
print_bool_end:
    jr      $ra
print_newline:
    la      $a0, newline
    li      $v0, 4
    syscall 
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
    syscall 
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
