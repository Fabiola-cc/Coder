package com.fmd;

/**
 * Descriptor de Registro
 *
 * Mantiene el estado de un registro MIPS:
 * - ¿Qué variable/temporal contiene?
 * - ¿Está "dirty" (modificado sin guardar en memoria)?
 * - ¿Está libre o en uso?
 *
 * BASADO EN: Dragon Book, Section 8.6 - Register Descriptors
 */
public class RegisterDescriptor {

    private final String registerName;  // Nombre del registro ($t0, $s1, etc.)
    private String variable;            // Variable actualmente almacenada (null si libre)
    private boolean dirty;              // true si modificado y no guardado en memoria
    private boolean locked;             // true si no puede ser desalojado (ej: en uso en instrucción)

    // ============================================
    // CONSTRUCTOR
    // ============================================
    public RegisterDescriptor(String registerName) {
        this.registerName = registerName;
        this.variable = null;
        this.dirty = false;
        this.locked = false;
    }

    // ============================================
    // ASIGNACIÓN Y LIBERACIÓN
    // ============================================

    /**
     * Asigna una variable a este registro
     */
    public void assign(String variable) {
        this.variable = variable;
        this.dirty = false; // recién asignado, no modificado aún
        this.locked = false;
    }

    /**
     * Libera el registro (lo marca como disponible)
     */
    public void free() {
        this.variable = null;
        this.dirty = false;
        this.locked = false;
    }

    /**
     * Verifica si el registro está libre
     */
    public boolean isFree() {
        return variable == null;
    }

    /**
     * Verifica si el registro está en uso
     */
    public boolean isOccupied() {
        return variable != null;
    }

    // ============================================
    // DIRTY BIT MANAGEMENT
    // ============================================

    /**
     * Marca el registro como "dirty" (modificado)
     * Esto significa que el valor en el registro difiere del valor en memoria
     */
    public void setDirty() {
        this.dirty = true;
    }

    /**
     * Marca el registro como "clean" (sincronizado con memoria)
     */
    public void setClean() {
        this.dirty = false;
    }

    /**
     * Verifica si el registro está dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    // ============================================
    // LOCKING (prevenir desalojo)
    // ============================================

    /**
     * Bloquea el registro (no puede ser desalojado)
     * Útil cuando el registro está siendo usado en la instrucción actual
     */
    public void lock() {
        this.locked = true;
    }

    /**
     * Desbloquea el registro
     */
    public void unlock() {
        this.locked = false;
    }

    /**
     * Verifica si el registro está bloqueado
     */
    public boolean isLocked() {
        return locked;
    }

    // ============================================
    // GETTERS
    // ============================================

    /**
     * Obtiene el nombre del registro
     */
    public String getRegisterName() {
        return registerName;
    }

    /**
     * Obtiene la variable almacenada (null si libre)
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Verifica si contiene una variable específica
     */
    public boolean contains(String varName) {
        return variable != null && variable.equals(varName);
    }

    // ============================================
    // UTILIDADES
    // ============================================

    /**
     * Copia el estado de otro descriptor
     */
    public void copyFrom(RegisterDescriptor other) {
        this.variable = other.variable;
        this.dirty = other.dirty;
        this.locked = other.locked;
    }

    /**
     * Resetea el descriptor a su estado inicial
     */
    public void reset() {
        free();
    }

    @Override
    public String toString() {
        if (isFree()) {
            return registerName + ": [FREE]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(registerName).append(": ");
        sb.append(variable);

        if (dirty) {
            sb.append(" [DIRTY]");
        }

        if (locked) {
            sb.append(" [LOCKED]");
        }

        return sb.toString();
    }

    /**
     * Representación detallada para debugging
     */
    public String toDetailedString() {
        return String.format(
                "RegisterDescriptor{reg=%s, var=%s, dirty=%b, locked=%b}",
                registerName,
                variable != null ? variable : "null",
                dirty,
                locked
        );
    }
}