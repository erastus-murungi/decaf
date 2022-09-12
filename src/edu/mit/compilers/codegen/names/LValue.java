package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;

public abstract class LValue extends Value {
    public LValue(Type type, String label) {
        super(type, label);
    }

    public abstract LValue copy();

    public static LValue gen(Type type) {
        if (type.equals(Type.Int) || type.equals(Type.Bool))
            return new VirtualRegister(TemporaryNameIndexGenerator.getNextTemporaryVariable(), type);
        else if (type.equals(Type.IntArray) || type.equals(Type.BoolArray))
            return new MemoryAddress(type, TemporaryNameIndexGenerator.getNextTemporaryVariable());
        else
            throw new IllegalArgumentException(type.getSourceCode());
    }
}
