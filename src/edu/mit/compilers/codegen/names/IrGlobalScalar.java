package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public class IrGlobalScalar extends IrGlobal {
    public IrGlobalScalar(String label, Type type) {
        super(label, type);
    }
}
