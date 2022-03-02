package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.BuiltinType;

public class VariableDescriptor extends Descriptor {
    public Object value;

    public VariableDescriptor(String id, Object value, BuiltinType type) {
        super(type, id);
        this.value = value;
    }
}
