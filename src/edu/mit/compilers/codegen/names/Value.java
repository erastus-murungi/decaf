package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public abstract class Value {
    protected Type type;

    protected String label;

    public Value(Type type, String label) {
        this.type = type;
        this.label = label;
    }

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Value that = (Value) o;
        return toString().equals(that.toString());
    }

    public abstract <T extends Value> T copy();

    public abstract void renameForSsa(int versionNumber);

    public abstract String repr();

}
