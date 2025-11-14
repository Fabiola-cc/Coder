package com.fmd.modules;

/**
 * Representa un registro MIPS
 */
public class Register {
    private String name;
    private boolean isTemporary;
    private int number;

    /**
     * Constructor para registros con nombre
     */
    public Register(String name) {
        this.name = name;
        this.isTemporary = name.startsWith("$t") || name.startsWith("$v") || name.startsWith("$a");
        this.number = extractNumber(name);
    }

    /**
     * Constructor para registros temporales numerados
     */
    public Register(int number, boolean isTemporary) {
        this.number = number;
        this.isTemporary = isTemporary;
        if (isTemporary) {
            this.name = "$t" + number;
        } else {
            this.name = "$s" + number;
        }
    }

    /**
     * Extrae el número del nombre del registro
     */
    private int extractNumber(String name) {
        if (name.matches(".*\\d+$")) {
            String digits = name.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                return Integer.parseInt(digits);
            }
        }
        return -1;
    }

    /**
     * Obtiene el nombre del registro
     */
    public String getName() {
        return name;
    }

    /**
     * Verifica si es un registro temporal
     */
    public boolean isTemporary() {
        return isTemporary;
    }

    /**
     * Obtiene el número del registro
     */
    public int getNumber() {
        return number;
    }

    /**
     * Verifica si es un registro especial (zero, sp, ra, etc.)
     */
    public boolean isSpecial() {
        return name.equals("$zero") || name.equals("$sp") ||
                name.equals("$ra") || name.equals("$fp") ||
                name.equals("$gp") || name.equals("$v0") || name.equals("$v1");
    }

    /**
     * Verifica si es un registro salvado ($s0-$s7)
     */
    public boolean isSaved() {
        return name.startsWith("$s");
    }

    /**
     * Verifica si es un registro de argumento ($a0-$a3)
     */
    public boolean isArgument() {
        return name.startsWith("$a");
    }

    /**
     * Verifica si es un registro de valor de retorno ($v0-$v1)
     */
    public boolean isReturnValue() {
        return name.equals("$v0") || name.equals("$v1");
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Register register = (Register) obj;
        return name.equals(register.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    // Registros predefinidos comunes
    public static final Register ZERO = new Register("$zero");
    public static final Register SP = new Register("$sp");
    public static final Register RA = new Register("$ra");
    public static final Register FP = new Register("$fp");
    public static final Register GP = new Register("$gp");
    public static final Register V0 = new Register("$v0");
    public static final Register V1 = new Register("$v1");
    public static final Register A0 = new Register("$a0");
    public static final Register A1 = new Register("$a1");
    public static final Register A2 = new Register("$a2");
    public static final Register A3 = new Register("$a3");

    // Registros temporales
    public static final Register T0 = new Register("$t0");
    public static final Register T1 = new Register("$t1");
    public static final Register T2 = new Register("$t2");
    public static final Register T3 = new Register("$t3");
    public static final Register T4 = new Register("$t4");
    public static final Register T5 = new Register("$t5");
    public static final Register T6 = new Register("$t6");
    public static final Register T7 = new Register("$t7");
    public static final Register T8 = new Register("$t8");
    public static final Register T9 = new Register("$t9");

    // Registros salvados
    public static final Register S0 = new Register("$s0");
    public static final Register S1 = new Register("$s1");
    public static final Register S2 = new Register("$s2");
    public static final Register S3 = new Register("$s3");
    public static final Register S4 = new Register("$s4");
    public static final Register S5 = new Register("$s5");
    public static final Register S6 = new Register("$s6");
    public static final Register S7 = new Register("$s7");
}