package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.AbstractName;

import java.util.Objects;

public abstract class UseDef {
    public final AbstractName variable;
    public final ThreeAddressCode line;

    public UseDef(AbstractName variable, ThreeAddressCode line) {
        this.variable = variable;
        this.line = line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UseDef useDef = (UseDef) o;
        return Objects.equals(variable, useDef.variable);
    }

    @Override
    public int hashCode() {
        return variable.hashCode();
    }
}
