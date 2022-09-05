package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.Value;

public class Use extends UseDef {
    public Use(Value variable, Instruction line) {
        super(variable, line);
    }


    @Override
    public String toString() {
        return "use " + variable.repr();
    }
}
