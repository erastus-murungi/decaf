package edu.mit.compilers.asm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum X64RegisterType {
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


    public static final Map<X64RegisterType, Integer> argumentRegistersOrdering = new HashMap<>();
    public static final int N_ARG_REGISTERS = 6;
    public static final X64RegisterType[] usedAsTemps = {R10, RAX, R11, R12};
    public static final List<X64RegisterType> parameterRegisters = List.of(RDI, RSI, RDX, RCX, R8, R9);
    public static final List<X64RegisterType> calleeSaved = List.of(RBX, R12, R13, R14, R15); //
    public static final List<X64RegisterType> callerSaved = List.of(R10, R11, RDI, RSI, RDX, RCX, R8, R9, RAX);
    // this is the order to allocate registers
    // note the argument registers are accessed in reverse order
        public static final List<X64RegisterType> regsToAllocate = List.of(R13, R14, R15, RDX, RCX, R9, R8, RSI, RDI); // use in this order
    public static final X64RegisterType[] availableRegs = {RBX, R12, R12, R14, R15, RDI, RSI, R8, R9, RDX, RCX, RAX, R10, R11}; // Use in this order

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

    public static int sortArgumentRegisters(X64RegisterType register1, X64RegisterType register2) {
        return argumentRegistersOrdering.get(register1).compareTo(argumentRegistersOrdering.get(register2));
    }

    @Override
    public String toString() {
        return "%" + super.toString()
                .toLowerCase();
    }

    public static X64RegisterType fromString(String string) {
        return Arrays.stream(X64RegisterType.class.getEnumConstants()).filter(x64Register -> x64Register.toString().equals(string)).findFirst().orElseThrow();
    }

}
