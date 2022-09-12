package edu.mit.compilers.dataflow.ssapasses;

import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.dataflow.OptimizationContext;
import edu.mit.compilers.dataflow.passes.OptimizationPass;

public abstract class SsaOptimizationPass<T> extends OptimizationPass {
    public SsaOptimizationPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }
}
