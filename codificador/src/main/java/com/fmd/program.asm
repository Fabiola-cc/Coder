.data
true_str: .asciiz "true"
newline: .asciiz "\n"
false_str: .asciiz "false"
space: .asciiz " "

.text
.globl main

    li      $t0, 314
    move    $t1, $t0
    move    $t2, $t3
    move    $t4, $t2
    li      $t5, 1
    move    $t6, $t5
    li      $t7, 2
    move    $t0, $t7
    li      $t2, 3
    move    $t2, $t5
    li      $t5, 4
    move    $t5, $t7
    li      $t7, 5
    move    $t7, $t1
    li      $t1, 1
    move    $t1, $t3
    li      $t3, 2
    move    $t3, $t4
    li      $t4, 3
    move    $t4, $t6
    li      $t6, 4
    move    $t6, $t0
makeAdder:
    li      $t0, 5
    add     $t0, $t0, $t2
    move    $v0, $t2
    j       epilog
makeAdder_epilog:
    move    $sp, $fp
    lw      $fp, 0($sp)
    lw      $ra, 4($sp)
    addi    $sp, $sp, 8
    jr      $ra
    li      $t2, 5
    move    $t2, $t2
    move    $a0, $t2
    jal     makeAdder
    move    $t2, $v0
    move    $t2, $t2
    move    $t2, $t2
    add     $t2, $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
    li      $t2, 5
    add     $t2, $t2, $t2
    beq     $t2, $t2, L1
    move    $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
    j       L2
L1:
    move    $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
L2:
L3:
    li      $t2, 10
    add     $t2, $t2, $t2
    beq     $t2, $t2, L4
    li      $t2, 1
    add     $t2, $t2, $t2
    move    $t2, $t2
    j       L3
L4:
L5:
    move    $t2, $t2
    add     $t2, $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
    li      $t2, 1
    sub     $t2, $t2, $t2
    move    $t2, $t2
    li      $t2, 7
    add     $t2, $t2, $t2
    bne     $t2, $t2, L5
L6:
    li      $t2, 0
    move    $t2, $t2
L7:
    li      $t2, 3
    add     $t2, $t2, $t2
    beq     $t2, $t2, L8
    move    $t2, $t2
    add     $t2, $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
    li      $t2, 1
    add     $t2, $t2, $t2
    move    $t2, $t2
    j       L7
L8:
    move    $t2, $t2
    li      $t2, 7
    beq     $t2, $t2, L10
    li      $t2, 6
    beq     $t2, $t2, L11
    j       L12
L10:
    move    $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
    j       L9
L11:
    move    $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
    j       L9
L12:
    move    $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
L9:
    # try_begin -> catch: L13
    li      $t2, 10
    move    $t2, $t2
    move    $t2, $t2
    move    $t2, $t2
    add     $t2, $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
    # try_end
    j       L14
L13:
    move    $t2, $t2
    move    $t2, $t2
    add     $t2, $t2, $t2
    move    $t2, $t2
    move    $a0, $t2
    jal     print
L14:
Animal:
    move    $t2, $t2
    move    $t2, $t2
constructor:
    move    $t2, $t2
constructor_epilog:
    move    $sp, $fp
    lw      $fp, 0($sp)
    lw      $ra, 4($sp)
    addi    $sp, $sp, 8
    jr      $ra
speak:
    move    $t0, $t0
    move    $t0, $t0
    add     $t0, $t0, $t0
    move    $v0, $t0
    j       epilog
speak_epilog:
    move    $sp, $fp
    lw      $fp, 0($sp)
    lw      $ra, 4($sp)
    addi    $sp, $sp, 8
    jr      $ra
    # Unsupported OpType: END_CLASS
Dog:
    # Unsupported OpType: END_CLASS
Cat:
    # Unsupported OpType: END_CLASS
