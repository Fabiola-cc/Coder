.data
newline: .asciiz "\n"
space: .asciiz " "
true_str: .asciiz "true"
false_str: .asciiz "false"
concat_buffer: .space 512
int_buffer: .space 32
str_0: .asciiz "Hola "
str_1: .asciiz "Juan"
str_2: .asciiz "\n"
.align 2
numbers: .space 24
.align 2
matrix: .space 20


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
    syscall 
    move    $s1, $v0
    move    $a0, $s1
    move    $a1, $t0
    move    $a2, $t3
    jal     Persona_constructor
    # Property get: t3 = p.edad
    lw      $t3, 8($s1)
    move    $a0, $t2
    jal     print
    la      $t3, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    li      $t3, 1
    sw      $t3, numbers+0
    li      $t3, 2
    sw      $t3, numbers+4
    li      $t3, 3
    sw      $t3, numbers+8
    li      $t3, 4
    sw      $t3, numbers+12
    li      $t3, 5
    sw      $t3, numbers+16
    li      $t3, 1
    sw      $t3, matrix+0
    li      $t3, 2
    sw      $t3, matrix+4
    li      $t3, 3
    sw      $t3, matrix+8
    li      $t3, 4
    sw      $t3, matrix+12
    li      $t3, 4
    sll     $t8, $t3, 2
    la      $t9, numbers
    add     $t8, $t9, $t8
    lw      $t2, 0($t8)
    move    $t1, $t2
    li      $t2, 1
    li      $t8, 2
    mul     $t8, $t2, $t8
    sll     $t8, $t8, 2
    la      $t3, matrix
    add     $t3, $t3, $t8
    li      $t2, 1
    sll     $t8, $t2, 2
    add     $t8, $t3, $t8
    lw      $t0, 0($t8)
    move    $t4, $t0
    la      $t0, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    move    $t0, $t1
    move    $a0, $t1
    li      $v0, 1
    syscall 
    la      $t0, str_2
    la      $a0, str_2
    li      $v0, 4
    syscall 
    move    $t0, $t4
    move    $a0, $t4
    li      $v0, 1
    syscall 

    # Fin del programa
    li      $v0, 10
    syscall

    # Class Persona
    # end Class Persona

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
    lw      $s0, 40($fp)
    # Loaded nombre from 40($fp) into $s0
    lw      $s1, 44($fp)
    # Loaded edad from 44($fp) into $s1
    # Property set: this.nombre = nombre
    lw      $t8, 0($fp)
    sw      $s0, 0($t8)
    # Property set: this.edad = edad
    lw      $t8, 0($fp)
    sw      $s1, 8($t8)
constructor_epilog:
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
