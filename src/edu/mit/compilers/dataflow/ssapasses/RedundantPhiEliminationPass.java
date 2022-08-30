package edu.mit.compilers.dataflow.ssapasses;

import java.util.Set;

import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;


/**
 * The redundant PHI instruction is defined as follows:
 * 1) x=phi(x,x,x) (remove only)
 * 2) x=phi(y,y,y) (regard as `x=y' and do copy propagation)
 * 3) x=phi(y,y,x) (regard as `x=y' and do copy propagation)
 */
public class RedundantPhiEliminationPass extends SsaOptimizationPass<Void> {
    public RedundantPhiEliminationPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    @Override
    public boolean runFunctionPass() {
        return false;
    }
}
