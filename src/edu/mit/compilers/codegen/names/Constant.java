package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public abstract class Constant extends Value {
    public Constant(Type type, String label) {
        super(type, label);
    }

    public abstract Object getValue();
}
