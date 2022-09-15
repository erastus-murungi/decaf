package decaf.codegen.names;

import decaf.ast.Type;

public abstract class IrAssignableValue extends IrValue {
    public IrAssignableValue(Type type, String label) {
        super(type, label);
    }

    public abstract IrAssignableValue copy();
}
