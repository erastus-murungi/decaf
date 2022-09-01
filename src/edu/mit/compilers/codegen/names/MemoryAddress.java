package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public class MemoryAddress extends Variable {
    public MemoryAddress(long index, Type type) {
        super(index, type);
    }

    public MemoryAddress(String label, Type type) {
        super(label, type);
    }


    @Override
    public String repr() {
        return String.format("*%s", getLabel());
    }

    public MemoryAddress copy() {
        return new MemoryAddress(label, type);
    }
}
