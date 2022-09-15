package decaf.dataflow.passes;

import decaf.dataflow.OptimizationContext;
import decaf.codegen.codes.Method;

public class InstructionStrengthReductionPass extends OptimizationPass {
    public InstructionStrengthReductionPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    @Override
    public boolean runFunctionPass() {
        return false;
    }
}
