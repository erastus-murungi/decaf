package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.BuiltinType;

public class MethodParameterDescriptor extends Descriptor {
    public MethodParameterDescriptor(String id, BuiltinType type) {
        super(type, id);
    }
}

