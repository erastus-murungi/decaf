package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.Type;

public class ParameterDescriptor extends Descriptor {
    public ParameterDescriptor(String id, Type type) {
        super(type, id);
    }
}

