package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;

public class MemoryAddressName extends TemporaryName {
    public MemoryAddressName(long index, long size, BuiltinType builtinType) {
        super(index, size, builtinType);
    }

    @Override
    public String repr() {
        return String.format("*%d", index);
    }
}
