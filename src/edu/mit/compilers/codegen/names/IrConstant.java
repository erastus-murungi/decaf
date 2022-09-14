package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public abstract class IrConstant extends IrValue {
    public IrConstant(Type type, String label) {
        super(type, label);
    }

    public abstract Object getValue();
}
