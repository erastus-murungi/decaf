package edu.mit.compilers.asm.operands;

import edu.mit.compilers.codegen.names.LValue;

public class X64GlobalOperand extends X64Operand {
    LValue name;

    public X64GlobalOperand(LValue name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name.toString(), "%rip");
    }
}
