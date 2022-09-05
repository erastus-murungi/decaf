package edu.mit.compilers.asm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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


    public static final Map<X64Register, Integer> argumentRegistersOrdering = new HashMap<>();
    public static final int N_ARG_REGISTERS = 6;
    public static final int N_AVAILABLE_REGISTERS = 10;
    public static final X64Register[] usedAsTemps = {R10, RAX, R11, R12};
    public static final X64Register[] parameterRegisters = {RDI, RSI, RDX, RCX, R8, R9};
    public static final X64Register[] calleeSaved = {RBX, R12, R13, R14, R15}; //
    public static final EnumSet<X64Register> callerSaved = EnumSet.of(R10, R11, RDI, RSI, RDX, RCX, R8, R9, RAX);
    // this is the order to allocate registers
    // note the argument registers are accessed in reverse order
    public static final X64Register[] regsToAllocate = {RBX, R13, R14, R15, RDX, RCX, R9, R8, RSI, RDI}; // use in this order
    public static final X64Register[] availableRegs = {RBX, R12, R12, R14, R15, RDI, RSI, R8, R9, RDX, RCX, RAX, R10, R11}; // Use in this order

    static {
        argumentRegistersOrdering.put(RDI, 1);
        argumentRegistersOrdering.put(RSI, 2);
        argumentRegistersOrdering.put(RDX, 3);
        argumentRegistersOrdering.put(RCX, 4);
        argumentRegistersOrdering.put(R8, 5);
        argumentRegistersOrdering.put(R9, 6);
        argumentRegistersOrdering.put(RBX, 7);
        argumentRegistersOrdering.put(RAX, 8);
        argumentRegistersOrdering.put(R10, 9);
        argumentRegistersOrdering.put(R11, 10);
        argumentRegistersOrdering.put(R12, 11);
        argumentRegistersOrdering.put(R13, 12);
        argumentRegistersOrdering.put(R14, 13);
        argumentRegistersOrdering.put(R15, 14);
        argumentRegistersOrdering.put(RBP, 15);
        argumentRegistersOrdering.put(RSP, 16);
        argumentRegistersOrdering.put(STACK, 17);
    }

    public static int sortArgumentRegisters(X64Register register1, X64Register register2) {
        return argumentRegistersOrdering.get(register1).compareTo(argumentRegistersOrdering.get(register2));
    }

    @Override
    public String toString() {
        return "%" + super.toString()
                .toLowerCase();
    }

}
