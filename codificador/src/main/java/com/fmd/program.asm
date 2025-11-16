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
.align 2
numbers: .space 24  # Array de 6 elementos
.align 2
matrix: .space 20  # Array de 5 elementos


.text
.globl main
main:
    move    $fp, $sp

    li      $t0, 314
    move    $t1, $t0
    la      $t0, str_0
    move    $t2, $t0
    li      $t0, 1
    la      $t3, numbers
    sw      $t0, 0($t3)
    li      $t0, 2
    la      $t3, numbers
    sw      $t0, 4($t3)
    li      $t0, 3
    la      $t3, numbers
    sw      $t0, 8($t3)
    li      $t0, 4
    la      $t3, numbers
    sw      $t0, 12($t3)
    li      $t0, 5
    la      $t3, numbers
    sw      $t0, 16($t3)
    li      $t0, 1
    la      $t4, matrix
    sw      $t0, 0($t4)
    li      $t0, 2
    la      $t4, matrix
    sw      $t0, 0($t4)
    li      $t0, 3
    la      $t4, matrix
    sw      $t0, 4($t4)
    li      $t0, 4
    la      $t4, matrix
    sw      $t0, 4($t4)
    li      $t2, 5
    move    $a0, $t2
    jal     makeAdder
    move    $t2, $v0
    move    $t1, $t2
    la      $t2, str_1
    add     $t0, $t2, $t1
    move    $a0, $t0
    jal     print
    li      $t2, 5
    add     $t1, $t0, $t2
    li      $t3, 0
    beq     $t1, $t3, L1
    la      $t2, str_2
    move    $a0, $t2
    jal     print
    j       L2
L1:
    la      $t2, str_3
    move    $a0, $t2
    jal     print
L2:
L3:
    li      $t2, 10
    add     $t1, $t0, $t2
    li      $t3, 0
    beq     $t1, $t3, L4
    li      $t2, 1
    add     $t4, $t0, $t2
    move    $t0, $t4
    j       L3
L4:
L5:
    la      $t1, str_4
    add     $t4, $t1, $t0
    move    $a0, $t4
    jal     print
    li      $t4, 1
    sub     $t2, $t1, $t4
    move    $t1, $t2
    li      $t2, 7
    add     $t4, $t1, $t2
    li      $t3, 0
    bne     $t4, $t3, L5
L6:
    li      $t4, 0
    move    $t0, $t4
L7:
    li      $t4, 3
    add     $t2, $t0, $t4
    li      $t3, 0
    beq     $t2, $t3, L8
    la      $t4, str_5
    add     $t5, $t4, $t0
    move    $a0, $t5
    jal     print
    li      $t5, 1
    add     $t4, $t0, $t5
    move    $t0, $t4
    j       L7
L8:
    move    $t2, $t1
    li      $t4, 7
    beq     $t2, $t4, L10
    li      $t5, 6
    beq     $t2, $t5, L11
    j       L12
L10:
    la      $t6, str_6
    move    $a0, $t6
    jal     print
    j       L9
L11:
    la      $t6, str_7
    move    $a0, $t6
    jal     print
    j       L9
L12:
    la      $t6, str_8
    move    $a0, $t6
    jal     print
L9:
    # try_begin -> catch: L13
    li      $t6, 10
    la      $t4, numbers
    sll     $t2, $t6, 2
    add     $t2, $t4, $t2
    lw      $t5, 0($t2)
    move    $t6, $t5
    la      $t5, str_9
    add     $t1, $t5, $t6
    move    $a0, $t1
    jal     print
    # try_end
    j       L14
L13:
    move    $t5, $t1
    la      $t7, str_10
    add     $t3, $t7, $t5
    move    $a0, $t3
    jal     print
L14:

    # Fin del programa
    li      $v0, 10
    syscall

    # Class Animal
    la      $t7, str_11
    move    $t3, $t7
    # end Class Animal

    # Class Cat
    # end Class Cat

constructor:
    addi    $sp, $sp, -8
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    move    $fp, $sp
    move    $t0, $t1
constructor_epilog:
    move    $sp, $fp
    lw      $fp, 0($sp)
    lw      $ra, 4($sp)
    addi    $sp, $sp, 8
    jr      $ra

speak:
    addi    $sp, $sp, -8
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    move    $fp, $sp
    move    $t0, $t1
    la      $t2, str_12
    add     $t3, $t0, $t2
    move    $v0, $t3
    j       speak_epilog
speak_epilog:
    move    $sp, $fp
    lw      $fp, 0($sp)
    lw      $ra, 4($sp)
    addi    $sp, $sp, 8
    jr      $ra

makeAdder:
    addi    $sp, $sp, -8
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    move    $fp, $sp
    li      $t0, 5
    add     $t1, $t2, $t0
    move    $v0, $t1
    j       makeAdder_epilog
makeAdder_epilog:
    move    $sp, $fp
    lw      $fp, 0($sp)
    lw      $ra, 4($sp)
    addi    $sp, $sp, 8
    jr      $ra

    # Class Dog
    # end Class Dog

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
