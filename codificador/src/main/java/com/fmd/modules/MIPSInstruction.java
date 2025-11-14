package com.fmd.modules;

/**
 * Representación de una instrucción MIPS
 *
 * Cada instrucción tiene:
 * - Opcode (ADD, LW, BEQ, etc.)
 * - Operandos (rd, rs, rt, immediate, label, etc.)
 *
 * Soporta diferentes formatos:
 * - Tipo R: add $t0, $t1, $t2
 * - Tipo I: lw $t0, 4($sp)
 * - Tipo J: j label
 */
public class MIPSInstruction {

    // OPCODES MIPS
    public enum OpCode {
        // Aritméticas
        ADD,    // add $rd, $rs, $rt
        ADDI,   // addi $rt, $rs, imm
        SUB,    // sub $rd, $rs, $rt
        SUBI,   // subi $rt, $rs, imm (pseudo)
        MUL,    // mul $rd, $rs, $rt
        DIV,    // div $rs, $rt (resultado en HI/LO)

        // Lógicas
        AND,    // and $rd, $rs, $rt
        ANDI,   // andi $rt, $rs, imm
        OR,     // or $rd, $rs, $rt
        ORI,    // ori $rt, $rs, imm
        XOR,    // xor $rd, $rs, $rt
        NOR,    // nor $rd, $rs, $rt
        NOT,    // not $rd, $rs (pseudo: nor $rd, $rs, $zero)

        // Memoria
        LW,     // lw $rt, offset($rs)
        SW,     // sw $rt, offset($rs)
        LA,     // la $rt, label (pseudo: load address)
        LI,     // li $rt, immediate (pseudo: load immediate)

        // Movimiento
        MOVE,   // move $rd, $rs (pseudo: add $rd, $rs, $zero)
        MFHI,   // mfhi $rd (move from HI)
        MFLO,   // mflo $rd (move from LO)

        // Comparación (set)
        SLT,    // slt $rd, $rs, $rt (set less than)
        SLTI,   // slti $rt, $rs, imm
        SEQ,    // seq $rd, $rs, $rt (pseudo)
        SNE,    // sne $rd, $rs, $rt (pseudo)
        SGT,    // sgt $rd, $rs, $rt (pseudo)
        SGE,    // sge $rd, $rs, $rt (pseudo)
        SLE,    // sle $rd, $rs, $rt (pseudo)

        // Branches condicionales
        BEQ,    // beq $rs, $rt, label
        BNE,    // bne $rs, $rt, label
        BGT,    // bgt $rs, $rt, label (pseudo)
        BGE,    // bge $rs, $rt, label (pseudo)
        BLT,    // blt $rs, $rt, label (pseudo)
        BLE,    // ble $rs, $rt, label (pseudo)
        BEQZ,   // beqz $rs, label
        BNEZ,   // bnez $rs, label

        // Saltos incondicionales
        J,      // j label
        JAL,    // jal label (jump and link)
        JR,     // jr $rs (jump register)

        // Stack
        PUSH,   // pseudo: sw + addi $sp
        POP,    // pseudo: lw + addi $sp

        // Sistema
        SYSCALL, // syscall
        NOP,     // nop

        // Pseudo-instrucción para etiquetas
        LABEL,   // label:
        COMMENT  // # comentario
    }
    
    // CAMPOS
    private OpCode opcode;
    private String rd;          // Registro destino
    private String rs;          // Registro fuente 1
    private String rt;          // Registro fuente 2
    private String immediate;   // Valor inmediato o offset
    private String label;       // Etiqueta para saltos
    private String comment;     // Comentario opcional

    // CONSTRUCTOR ÚNICO

    /**
     * Constructor privado - usar métodos estáticos para crear
     */
    private MIPSInstruction(OpCode opcode) {
        this.opcode = opcode;
    }

    // MÉTODOS FACTORY (CONSTRUCTORES ESTÁTICOS)

    /**
     * Tipo R: add $rd, $rs, $rt
     */
    public static MIPSInstruction typeR(OpCode opcode, String rd, String rs, String rt) {
        MIPSInstruction instr = new MIPSInstruction(opcode);
        instr.rd = rd;
        instr.rs = rs;
        instr.rt = rt;
        return instr;
    }

    /**
     * Tipo I con inmediato: addi $rd, $rs, imm
     */
    public static MIPSInstruction typeI(OpCode opcode, String rd, String rs, int immediate) {
        MIPSInstruction instr = new MIPSInstruction(opcode);
        instr.rd = rd;
        instr.rs = rs;
        instr.immediate = String.valueOf(immediate);
        return instr;
    }

    /**
     * Load/Store: lw $rt, offset($base)
     */
    public static MIPSInstruction loadStore(OpCode opcode, String rt, String offsetBase) {
        MIPSInstruction instr = new MIPSInstruction(opcode);
        instr.rt = rt;
        instr.immediate = offsetBase; // "4($sp)"
        return instr;
    }

    /**
     * Branch: beq $rs, $rt, label
     */
    public static MIPSInstruction branch(OpCode opcode, String rs, String rt, String label) {
        MIPSInstruction instr = new MIPSInstruction(opcode);
        instr.rs = rs;
        instr.rt = rt;
        instr.label = label;
        return instr;
    }

    /**
     * Branch unario: beqz $rs, label
     */
    public static MIPSInstruction branchUnary(OpCode opcode, String rs, String label) {
        MIPSInstruction instr = new MIPSInstruction(opcode);
        instr.rs = rs;
        instr.label = label;
        return instr;
    }

    /**
     * Salto: j label / jal label
     */
    public static MIPSInstruction jump(OpCode opcode, String label) {
        MIPSInstruction instr = new MIPSInstruction(opcode);
        instr.label = label;
        return instr;
    }

    /**
     * Jump register: jr $rs
     */
    public static MIPSInstruction jumpReg(String rs) {
        MIPSInstruction instr = new MIPSInstruction(OpCode.JR);
        instr.rs = rs;
        return instr;
    }

    /**
     * Move: move $rd, $rs
     */
    public static MIPSInstruction move(String rd, String rs) {
        MIPSInstruction instr = new MIPSInstruction(OpCode.MOVE);
        instr.rd = rd;
        instr.rs = rs;
        return instr;
    }

    /**
     * Load immediate: li $rd, value
     */
    public static MIPSInstruction li(String rd, int value) {
        MIPSInstruction instr = new MIPSInstruction(OpCode.LI);
        instr.rd = rd;
        instr.immediate = String.valueOf(value);
        return instr;
    }

    /**
     * Load address: la $rd, label
     */
    public static MIPSInstruction la(String rd, String label) {
        MIPSInstruction instr = new MIPSInstruction(OpCode.LA);
        instr.rd = rd;
        instr.label = label;
        return instr;
    }

    /**
     * Etiqueta: label:
     */
    public static MIPSInstruction label(String name) {
        MIPSInstruction instr = new MIPSInstruction(OpCode.LABEL);
        instr.label = name;
        return instr;
    }

    /**
     * Comentario: # texto
     */
    public static MIPSInstruction comment(String text) {
        MIPSInstruction instr = new MIPSInstruction(OpCode.COMMENT);
        instr.comment = text;
        return instr;
    }

    /**
     * Syscall
     */
    public static MIPSInstruction syscall() {
        return new MIPSInstruction(OpCode.SYSCALL);
    }

    /**
     * NOP
     */
    public static MIPSInstruction nop() {
        return new MIPSInstruction(OpCode.NOP);
    }

    // GETTERS Y SETTERS
    public OpCode getOpcode() { return opcode; }
    public String getRd() { return rd; }
    public String getRs() { return rs; }
    public String getRt() { return rt; }
    public String getImmediate() { return immediate; }
    public String getLabel() { return label; }
    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    // GENERACIÓN DE CÓDIGO
    /**
     * Convierte la instrucción a su representación textual MIPS
     * Este método es CRÍTICO para debugging y generación del .asm
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Casos especiales
        if (opcode == OpCode.LABEL) {
            return label + ":";
        }

        if (opcode == OpCode.COMMENT) {
            return "    # " + comment;
        }

        // Instrucciones normales: agregar tabulación
        sb.append("    ");

        // Opcode en minúsculas
        String op = opcode.toString().toLowerCase();
        sb.append(op);

        // Espaciado para alineación (8 caracteres)
        while (sb.length() < 12) {
            sb.append(" ");
        }

        // Operandos según tipo de instrucción
        switch (opcode) {
            // Tipo R: add $rd, $rs, $rt
            case ADD: case SUB: case MUL: case AND: case OR: case XOR: case NOR:
            case SLT: case SEQ: case SNE: case SGT: case SGE: case SLE:
                sb.append(rd).append(", ").append(rs).append(", ").append(rt);
                break;

            // Tipo I con inmediato: addi $rt, $rs, imm
            case ADDI: case SUBI: case ANDI: case ORI: case SLTI:
                sb.append(rd).append(", ").append(rs).append(", ").append(immediate);
                break;

            // Load/Store: lw $rt, offset($base)
            case LW: case SW:
                sb.append(rt).append(", ").append(immediate);
                break;

            // Load immediate: li $rt, value
            case LI:
                sb.append(rd).append(", ").append(immediate);
                break;

            // Load address: la $rt, label
            case LA:
                sb.append(rd).append(", ").append(label);
                break;

            // Move: move $rd, $rs
            case MOVE:
                sb.append(rd).append(", ").append(rs);
                break;

            // Unario: not $rd, $rs
            case NOT: case MFHI: case MFLO:
                sb.append(rd).append(", ").append(rs);
                break;

            // Division: div $rs, $rt
            case DIV:
                sb.append(rs).append(", ").append(rt);
                break;

            // Branches: beq $rs, $rt, label
            case BEQ: case BNE: case BGT: case BGE: case BLT: case BLE:
                sb.append(rs).append(", ").append(rt).append(", ").append(label);
                break;

            // Branches unarios: beqz $rs, label
            case BEQZ: case BNEZ:
                sb.append(rs).append(", ").append(label);
                break;

            // Saltos: j label
            case J: case JAL:
                sb.append(label);
                break;

            // Jump register: jr $rs
            case JR:
                sb.append(rs);
                break;

            // Syscall y NOP
            case SYSCALL: case NOP:
                // No tienen operandos
                break;

            default:
                sb.append("# UNKNOWN OPCODE");
        }

        // Agregar comentario si existe
        if (comment != null && !comment.isEmpty()) {
            // Padding para alinear comentarios
            while (sb.length() < 40) {
                sb.append(" ");
            }
            sb.append("# ").append(comment);
        }

        return sb.toString();
    }

    /**
     * Versión compacta para debugging
     */
    public String toShortString() {
        String op = opcode.toString().toLowerCase();

        if (opcode == OpCode.LABEL) {
            return label + ":";
        }

        if (rd != null && rs != null && rt != null) {
            return op + " " + rd + ", " + rs + ", " + rt;
        } else if (rd != null && rs != null) {
            return op + " " + rd + ", " + rs;
        } else if (rd != null) {
            return op + " " + rd;
        } else {
            return op + " " + label;
        }
    }
}