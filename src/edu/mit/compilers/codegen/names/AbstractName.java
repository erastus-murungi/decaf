package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public abstract class AbstractName {
    protected Type type;
    protected String label;

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public AbstractName(Type type, String label) {
        this.type = type;
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractName that = (AbstractName) o;
        return toString().equals(that.toString());
    }

    public abstract <T extends AbstractName> T copy();

    public abstract String repr();

}
