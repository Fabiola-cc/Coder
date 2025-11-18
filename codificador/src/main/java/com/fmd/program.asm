.data
newline: .asciiz "\n"
space: .asciiz " "
true_str: .asciiz "true"
false_str: .asciiz "false"
concat_buffer: .space 512
int_buffer: .space 32
.align 2
numbers: .space 24
.align 2
matrix: .space 20


.text
.globl main
main:
    move    $fp, $sp

    li      $t0, 1
    # Array store: numbers[0] = t1
    sw      $t0, numbers+0
    li      $t0, 2
    # Array store: numbers[1] = t1
    sw      $t0, numbers+4
    li      $t0, 3
    # Array store: numbers[2] = t1
    sw      $t0, numbers+8
    li      $t0, 4
    # Array store: numbers[3] = t1
    sw      $t0, numbers+12
    li      $t0, 5
    # Array store: numbers[4] = t1
    sw      $t0, numbers+16
    li      $t0, 1
    sw      $t0, matrix+0
    li      $t0, 2
    sw      $t0, matrix+4
    li      $t0, 3
    sw      $t0, matrix+8
    li      $t0, 4
    sw      $t0, matrix+12
    li      $t0, 1
    # Array load: t2 = matrix[t1]
    sll     $t2, $t0, 2
    la      $t3, matrix
    add     $t2, $t3, $t2
    lw      $t1, 0($t2)
    li      $t0, 1
    # ERROR: Array t2 not found
    move    $t5, $t4
    move    $t4, $t5
    move    $a0, $t4
    li      $v0, 1
    syscall 

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
