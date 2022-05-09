package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.AbstractName;

public class Use extends UseDef {
    public Use(AbstractName variable, Instruction line) {
        super(variable, line);
    }


    @Override
    public String toString() {
        return "use "+ variable.repr();
    }
}
