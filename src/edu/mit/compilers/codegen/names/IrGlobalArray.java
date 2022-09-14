package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public class IrGlobalArray extends IrGlobal {
    public IrGlobalArray(String label, Type type) {
        super(label, type);
    }
}
