.data
true_str: .asciiz "true"
newline: .asciiz "\n"
false_str: .asciiz "false"
space: .asciiz " "

.text
.globl main

    li      $t0, 314
    move    $t1, $t0
    move    $t0, $t2
    move    $t3, $t0
    li      $t0, 1
    move    $t4, $t0
    li      $t0, 2
    move    $t5, $t0
    li      $t0, 3
    move    $t6, $t0
    li      $t0, 4
    move    $t7, $t0
    li      $t0, 5
    move    $t0, $t0
    li      $t0, 1
    move    $t0, $t0
    li      $t0, 2
    move    $t0, $t0
    li      $t0, 3
    move    $t0, $t0
    li      $t0, 4
    move    $t0, $t0
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
    move    $t1, $t3
    add     $t4, $t1, $t0
    move    $a0, $t4
    jal     print
    li      $t4, 5
    add     $t1, $t0, $t4
    beq     $t1, $t5, L1
    move    $t4, $t6
    move    $a0, $t4
    jal     print
    j       L2
L1:
    move    $t4, $t1
    move    $a0, $t4
    jal     print
L2:
L3:
    li      $t4, 10
    add     $t0, $t7, $t4
    beq     $t0, $t5, L4
    li      $t4, 1
    add     $t0, $t7, $t4
    move    $t7, $t0
    j       L3
L4:
L5:
    move    $t0, $t0
    add     $t0, $t0, $t7
    move    $a0, $t0
    jal     print
    li      $t0, 1
    sub     $t7, $t4, $t0
    move    $t4, $t7
    li      $t7, 7
    add     $t0, $t4, $t7
    bne     $t0, $t5, L5
L6:
    li      $t0, 0
    move    $t0, $t0
L7:
    li      $t0, 3
    add     $t7, $t0, $t0
    beq     $t7, $t5, L8
    move    $t0, $t7
    add     $t7, $t0, $t0
    move    $a0, $t7
    jal     print
    li      $t7, 1
    add     $t0, $t0, $t7
    move    $t0, $t0
    j       L7
L8:
    move    $t4, $t4
    li      $t0, 7
    beq     $t4, $t0, L10
    li      $t7, 6
    beq     $t4, $t7, L11
    j       L12
L10:
    move    $t4, $t4
    move    $a0, $t4
    jal     print
    j       L9
L11:
    move    $t4, $t7
    move    $a0, $t4
    jal     print
    j       L9
L12:
    move    $t4, $t2
    move    $a0, $t4
    jal     print
L9:
    # try_begin -> catch: L13
    li      $t3, 10
    move    $t3, $t3
    move    $t6, $t3
    move    $t3, $t1
    add     $t5, $t3, $t6
    move    $a0, $t5
    jal     print
    # try_end
    j       L14
L13:
    move    $t5, $t3
    move    $t0, $t0
    add     $t3, $t0, $t5
    move    $a0, $t3
    jal     print
L14:
    # Class Animal
    move    $t3, $t0
    move    $t4, $t3
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
    move    $t2, $t3
    add     $t4, $t0, $t2
    move    $v0, $t4
    j       speak_epilog
speak_epilog:
    move    $sp, $fp
    lw      $fp, 64($sp)
    lw      $ra, 68($sp)
    addi    $sp, $sp, 72
    jr      $ra
    # Unsupported OpType: END_CLASS
    # Class Dog
    # Unsupported OpType: END_CLASS
    # Class Cat
    # Unsupported OpType: END_CLASS
