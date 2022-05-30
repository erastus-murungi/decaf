package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public class MemoryAddressName extends TemporaryName {
    public MemoryAddressName(long index, Type type) {
        super(index, type);
    }

    public MemoryAddressName(String label, Type type) {
        super(label, type);
    }


    @Override
    public String repr() {
        return String.format("*%s", getLabel());
    }

    public MemoryAddressName copy() {
        return new MemoryAddressName(label, type);
    }
}
