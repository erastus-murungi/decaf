package edu.mit.compilers.dataflow.passes;

import java.util.Set;

import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.names.AbstractName;

public class InstructionStrengthReductionPass extends OptimizationPass {
    public InstructionStrengthReductionPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    @Override
    public boolean run() {
        return false;
    }
}
