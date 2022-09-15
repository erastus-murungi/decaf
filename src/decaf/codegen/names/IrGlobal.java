package decaf.codegen.names;

import decaf.ast.Type;

public abstract class IrGlobal extends IrAssignableValue {
    public IrGlobal(String label, Type type) {
        super(type, label);
    }
}
