package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public abstract class IrAssignableValue extends IrValue {
    public IrAssignableValue(Type type, String label) {
        super(type, label);
    }

    public abstract IrAssignableValue copy();
}
