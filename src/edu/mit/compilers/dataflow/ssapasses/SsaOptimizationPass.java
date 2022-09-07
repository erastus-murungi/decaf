package edu.mit.compilers.dataflow.ssapasses;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.dataflow.OptimizationContext;
import edu.mit.compilers.dataflow.passes.OptimizationPass;
import edu.mit.compilers.utils.TarjanSCC;

public abstract class SsaOptimizationPass<T> extends OptimizationPass {
    public SsaOptimizationPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }
}
