package edu.mit.compilers.asm;

public enum X64Register {
    RAX,
    RBX,
    RSP,
    RBP,
    RCX,
    RDX,
    RSI,
    RDI,
    R8,
    R9,
    R10,
    R11,
    R12,
    R13,
    R14,
    R15, al;

    @Override
    public String toString() {
        return "%" + super.toString().toLowerCase();
    }

    public static final int N_ARG_REGISTERS = 6;

    public static final X64Register[] argumentRegs = { RDI, RSI, RDX, RCX, R8, R9 };
    public static final X64Register[] calleeSaved = { RBX, R12, R13, R14, R15 }; //
    public static final X64Register[] callerSaved = { R10, R11, RDI, RSI, RDX, RCX, R8, R9, RAX };
    public static final X64Register[] availableRegs = { RBX, R12, R13, R14, R15, RDI, RSI, R8, R9, RDX, RCX, RAX, R10, R11 }; // Use in this order

}
