package decaf.descriptors;

import decaf.ast.Type;
import decaf.ast.Name;

public class VariableDescriptor extends Descriptor {
    public final Name name;

    public VariableDescriptor(String id, Type type, Name name) {
        super(type, id);
        this.name = name;
    }
}
