package decaf.descriptors;

import decaf.ast.Type;

public class ParameterDescriptor extends Descriptor {
    public ParameterDescriptor(String id, Type type) {
        super(type, id);
    }
}

