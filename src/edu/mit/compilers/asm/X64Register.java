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
    R15,
    STACK,

    al,

    EAX;



    @Override
    public String toString() {
        return "%" + super.toString().toLowerCase();
    }

    public static final int N_ARG_REGISTERS = 6;
    public static final int N_AVAILABLE_REGISTERS = 10;


    public static final X64Register[] usedAsTemps = { R10, RAX, R11, R12};
    public static final X64Register[] argumentRegs = { RDI, RSI, RDX, RCX, R8, R9 };
    public static final X64Register[] calleeSaved = { RBX, R12, R13, R14, R15 }; //
    public static final X64Register[] callerSaved = { R10, R11, RDI, RSI, RDX, RCX, R8, R9, RAX };
    // this is the order to allocate registers
    // note the argument registers are accessed in reverse order
    public static final X64Register[] regsToAllocate = { RBX, R13, R14, R15, RDX, RCX, R9, R8, RSI, RDI}; // use in this order
    public static final X64Register[] availableRegs = { RBX, R12, R13, R14, R15, RDI, RSI, R8, R9, RDX, RCX, RAX, R10, R11 }; // Use in this order

}
