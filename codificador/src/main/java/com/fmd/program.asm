.data
newline: .asciiz "\n"
space: .asciiz " "
true_str: .asciiz "true"
false_str: .asciiz "false"
concat_buffer: .space 512
int_buffer: .space 32
str_0: .asciiz " makes a sound"
str_1: .asciiz " says Woof!"
str_2: .asciiz "Max"
str_3: .asciiz "Golden"


.text
.globl main
main:
    move    $fp, $sp

    la      $t1, str_2
    move    $t0, $t1
    li      $t2, 3
    move    $t3, $t2
    la      $t4, str_3
    move    $t5, $t4
    # new Dog
    li      $a0, 32
    li      $v0, 9
    syscall 
    move    $s1, $v0
    move    $a0, $s1
    move    $a1, $t0
    move    $a2, $t3
    move    $a3, $t5
    jal     Dog_constructor
    # Property get: t1 = myDog.name
    lw      $t5, 0($s1)
    move    $a0, $t4
    jal     print

    # Fin del programa
    li      $v0, 10
    syscall

    # Class Animal
    # end Class Animal

Dog_bark:
    addi    $sp, $sp, -12
    sw      $fp, 4($sp)
    sw      $ra, 8($sp)
    move    $fp, $sp
    sw      $a0, 0($fp)
    # Property get: t3 = this.name
    lw      $t0, 0($a0)
    la      $t1, str_1
    # String concat: t1 = t3 + t2
    move    $a0, $t0
    move    $a1, $t1
    jal     concat_strings
    move    $t1, $v0
    move    $v0, $t1
    j       bark_epilog
bark_epilog:
    move    $sp, $fp
    lw      $fp, 4($sp)
    lw      $ra, 8($sp)
    addi    $sp, $sp, 12
    jr      $ra

Dog_constructor:
    addi    $sp, $sp, -44
    sw      $fp, 36($sp)
    sw      $ra, 40($sp)
    move    $fp, $sp
    sw      $a0, 0($fp)
    sw      $a1, 4($fp)
    sw      $a2, 8($fp)
    sw      $a3, 12($fp)
    # Property set: this.name = name
    lw      $t8, 0($fp)
    sw      $t0, 0($t8)
    # Property set: this.age = age
    lw      $t8, 0($fp)
    sw      $t1, 8($t8)
    # Property set: this.breed = breed
    lw      $t8, 0($fp)
    sw      $t2, 20($t8)
constructor_epilog:
    move    $sp, $fp
    lw      $fp, 36($sp)
    lw      $ra, 40($sp)
    addi    $sp, $sp, 44
    jr      $ra

Animal_speak:
    addi    $sp, $sp, -12
    sw      $fp, 4($sp)
    sw      $ra, 8($sp)
    move    $fp, $sp
    sw      $a0, 0($fp)
    # Property get: t1 = this.name
    lw      $t0, 0($a0)
    la      $t1, str_0
    # String concat: t3 = t1 + t2
    move    $a0, $t0
    move    $a1, $t1
    jal     concat_strings
    move    $t1, $v0
    move    $v0, $t1
    j       speak_epilog
speak_epilog:
    move    $sp, $fp
    lw      $fp, 4($sp)
    lw      $ra, 8($sp)
    addi    $sp, $sp, 12
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
