package edu.mit.compilers.dataflow.passes;

import java.util.Set;

import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.AbstractName;

public class InstructionStrengthReductionPass extends OptimizationPass {
    public InstructionStrengthReductionPass(Set<AbstractName> globalVariables, Method method) {
        super(globalVariables, method);
    }

    @Override
    public boolean run() {
        return false;
    }
}
