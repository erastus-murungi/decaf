package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public abstract class IrGlobal extends IrAssignableValue {
    public IrGlobal(String label, Type type) {
        super(type, label);
    }
}
