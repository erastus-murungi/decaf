package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.Name;

public class VariableDescriptor extends Descriptor {
    public final Name name;

    public VariableDescriptor(String id, BuiltinType type, Name name) {
        super(type, id);
        this.name = name;
    }
}
