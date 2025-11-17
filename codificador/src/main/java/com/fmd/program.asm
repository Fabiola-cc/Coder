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
    li      $t0, 5
    move    $a0, $t0
    jal     makeAdder
    move    $t0, $v0
    move    $t2, $t0
    la      $t0, str_1
    # String concat: t1 = t2 + addFive
    move    $a0, $t2
    jal     int_to_string
    move    $t1, $v0
    move    $a0, $t0
    move    $a1, $t1
    jal     concat_strings
    move    $t2, $v0
    move    $a0, $t2
    jal     print
    li      $t2, 5
    slt     $t0, $t2, $t3
    li      $t4, 0
    beq     $t0, $t4, L1
    la      $t2, str_2
    move    $a0, $t2
    jal     print
    j       L2
L1:
    la      $t3, str_3
    move    $a0, $t3
    jal     print
L2:
L3:
    li      $t3, 10
    slt     $t2, $t0, $t3
    li      $t4, 0
    beq     $t2, $t4, L4
    li      $t3, 1
    # String concat: t3 = addFive + t2
    move    $a0, $t0
    jal     int_to_string
    move    $t1, $v0
    move    $a0, $t1
    move    $a1, $t3
    jal     concat_strings
    move    $t3, $v0
    move    $t2, $t3
    j       L3
L4:
L5:
    la      $t0, str_4
    # String concat: t3 = t1 + addFive
    move    $a0, $t2
    jal     int_to_string
    move    $t1, $v0
    move    $a0, $t0
    move    $a1, $t1
    jal     concat_strings
    move    $t3, $v0
    move    $a0, $t3
    jal     print
    li      $t3, 1
    sub     $t2, $t0, $t3
    move    $t0, $t2
    li      $t2, 7
    slt     $t3, $t2, $t0
    li      $t4, 0
    bne     $t3, $t4, L5
L6:
    li      $t3, 0
    move    $t5, $t3
L7:
    li      $t3, 3
    slt     $t2, $t5, $t3
    li      $t4, 0
    beq     $t2, $t4, L8
    la      $t3, str_5
    # String concat: t2 = t3 + i
    move    $a0, $t3
    move    $a1, $t5
    jal     concat_strings
    move    $t3, $v0
    move    $a0, $t3
    jal     print
    li      $t3, 1
    # String concat: t3 = i + t2
    move    $a0, $t5
    move    $a1, $t3
    jal     concat_strings
    move    $t3, $v0
    move    $t5, $t3
    j       L7
L8:
    move    $t2, $t0
    li      $t3, 7
    beq     $t2, $t3, L10
    li      $t6, 6
    beq     $t2, $t6, L11
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
    la      $t3, numbers
    sll     $t2, $t7, 2
    add     $t2, $t3, $t2
    lw      $t6, 0($t2)
    move    $t7, $t6
    la      $t6, str_9
    # String concat: t2 = t1 + risky
    move    $a0, $t6
    move    $a1, $t7
    jal     concat_strings
    move    $t6, $v0
    move    $a0, $t6
    jal     print
    # try_end
    j       L14
L13:
    move    $t6, $t0
    la      $t1, str_10
    # String concat: t1 = t2 + err
    move    $a0, $t1
    move    $a1, $t6
    jal     concat_strings
    move    $t1, $v0
    move    $a0, $t1
    jal     print
L14:

    # Fin del programa
    li      $v0, 10
    syscall

    # Class Animal
    la      $t1, str_11
    # end Class Animal

    # Class Cat
    # end Class Cat

constructor:
    addi    $sp, $sp, -8
    sw      $fp, 0($sp)
    sw      $ra, 4($sp)
    move    $fp, $sp
    sw      $a0, 0($fp)
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
    # String concat: t4 = t1 + t2
    move    $a0, $t0
    move    $a1, $t2
    jal     concat_strings
    move    $t2, $v0
    move    $v0, $t2
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
    sw      $a0, 0($fp)
    li      $t0, 5
    # String concat: t2 = x + t1
    move    $a0, $t1
    jal     int_to_string
    move    $t1, $v0
    move    $a0, $t1
    move    $a1, $t0
    jal     concat_strings
    move    $t0, $v0
    move    $v0, $t0
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
