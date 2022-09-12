package edu.mit.compilers.dataflow.passes;

import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.dataflow.OptimizationContext;

public class InstructionStrengthReductionPass extends OptimizationPass {
    public InstructionStrengthReductionPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    @Override
    public boolean runFunctionPass() {
        return false;
    }
}
