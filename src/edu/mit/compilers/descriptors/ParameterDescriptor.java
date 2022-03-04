package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.BuiltinType;

public class ParameterDescriptor extends Descriptor {
    public ParameterDescriptor(String id, BuiltinType type) {
        super(type, id);
    }
}

