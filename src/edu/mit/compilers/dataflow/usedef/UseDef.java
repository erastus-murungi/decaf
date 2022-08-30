package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.Value;

import java.util.Objects;

public abstract class UseDef {
    public final Value variable;
    public final Instruction line;

    public UseDef(Value variable, Instruction line) {
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
