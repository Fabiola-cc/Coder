.data
newline: .asciiz "\n"
space: .asciiz " "
true_str: .asciiz "true"
false_str: .asciiz "false"
str_0: .asciiz "Hello, Compiscript!"
str_1: .asciiz "5 + 1 = "
str_2: .asciiz "Greater than 5"
str_3: .asciiz "5 or less"
str_4: .asciiz "Result is now "
str_5: .asciiz "Loop index: "
str_6: .asciiz "It's seven"
str_7: .asciiz "It's six"
str_8: .asciiz "Something else"
str_9: .asciiz "Risky access: "
str_10: .asciiz "Caught an error: "
str_11: .asciiz "hugo"
str_12: .asciiz " makes a sound."


.text
.globl main
main:
    # Inicialización del programa
    move    $fp, $sp

    li      $t0, 314
    move    $t1, $t0
    la      $t0, str_0
    move    $t2, $t0
    li      $t0, 1
    sw      $t0, 21($sp)
    li      $t0, 2
    sw      $t0, 25($sp)
    li      $t0, 3
    sw      $t0, 29($sp)
    li      $t0, 4
    sw      $t0, 33($sp)
    li      $t0, 5
    sw      $t0, 37($sp)
    li      $t0, 1
    sw      $t0, 53($sp)
    li      $t0, 2
    sw      $t0, 53($sp)
    li      $t0, 3
    sw      $t0, 57($sp)
    li      $t0, 4
    sw      $t0, 57($sp)
makeAdder:
    addi    $sp, $sp, -72
    sw      $fp, 64($sp)
    sw      $ra, 68($sp)
    move    $fp, $sp
    li      $t0, 5
    add     $t1, $t2, $t0
    move    $v0, $t1
    j       makeAdder_epilog
makeAdder_epilog:
    move    $sp, $fp
    lw      $fp, 64($sp)
    lw      $ra, 68($sp)
    addi    $sp, $sp, 72
    jr      $ra
    li      $t1, 5
    move    $a0, $t1
    jal     makeAdder
    move    $t1, $v0
    move    $t0, $t1
    la      $t1, str_1
    add     $t3, $t1, $t0
    move    $a0, $t3
    jal     print
    li      $t3, 5
    add     $t1, $t0, $t3
    li      $t4, 0
    beq     $t1, $t4, L1
    la      $t3, str_2
    move    $a0, $t3
    jal     print
    j       L2
L1:
    la      $t3, str_3
    move    $a0, $t3
    jal     print
L2:
L3:
    li      $t3, 10
    add     $t1, $t0, $t3
    li      $t4, 0
    beq     $t1, $t4, L4
    li      $t3, 1
    add     $t5, $t0, $t3
    move    $t0, $t5
    j       L3
L4:
L5:
    la      $t1, str_4
    add     $t5, $t1, $t0
    move    $a0, $t5
    jal     print
    li      $t5, 1
    sub     $t3, $t1, $t5
    move    $t1, $t3
    li      $t3, 7
    add     $t5, $t1, $t3
    li      $t4, 0
    bne     $t5, $t4, L5
L6:
    li      $t5, 0
    move    $t0, $t5
L7:
    li      $t5, 3
    add     $t3, $t0, $t5
    li      $t4, 0
    beq     $t3, $t4, L8
    la      $t5, str_5
    add     $t6, $t5, $t0
    move    $a0, $t6
    jal     print
    li      $t6, 1
    add     $t5, $t0, $t6
    move    $t0, $t5
    j       L7
L8:
    move    $t3, $t1
    li      $t5, 7
    beq     $t3, $t5, L10
    li      $t6, 6
    beq     $t3, $t6, L11
    j       L12
L10:
    la      $t7, str_6
    move    $a0, $t7
    jal     print
    j       L9
L11:
    la      $t7, str_7
    move    $a0, $t7
    jal     print
    j       L9
L12:
    la      $t7, str_8
    move    $a0, $t7
    jal     print
L9:
    # try_begin -> catch: L13
    li      $t7, 10
    addi    $t5, $t7, 0
    addi    $t5, $t5, 21
    lw      $t6, 0($t5)
    move    $t3, $t6
    la      $t6, str_9
    add     $t7, $t6, $t3
    move    $a0, $t7
    jal     print
    # try_end
    j       L14
L13:
    move    $t7, $t6
    la      $t1, str_10
    add     $t1, $t1, $t7
    move    $a0, $t1
    jal     print
L14:
    # Class Animal
    la      $t1, str_11
constructor:
    addi    $sp, $sp, -72
    sw      $fp, 64($sp)
    sw      $ra, 68($sp)
    move    $fp, $sp
    move    $t0, $t1
constructor_epilog:
    move    $sp, $fp
    lw      $fp, 64($sp)
    lw      $ra, 68($sp)
    addi    $sp, $sp, 72
    jr      $ra
speak:
    addi    $sp, $sp, -72
    sw      $fp, 64($sp)
    sw      $ra, 68($sp)
    move    $fp, $sp
    move    $t0, $t1
    la      $t2, str_12
    add     $t3, $t0, $t2
    move    $v0, $t3
    j       speak_epilog
speak_epilog:
    move    $sp, $fp
    lw      $fp, 64($sp)
    lw      $ra, 68($sp)
    addi    $sp, $sp, 72
    jr      $ra
    # end Class Animal
    # Class Dog
    # end Class Dog
    # Class Cat
    # end Class Cat

    # Fin del programa
    li      $v0, 10
    syscall
