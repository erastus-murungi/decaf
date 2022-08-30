package edu.mit.compilers.dataflow.ssapasses;

import java.util.Set;

import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;

public class SccpSsaPass extends SsaOptimizationPass<Void> {
    public SccpSsaPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    @Override
    public boolean runFunctionPass() {
        SCCP sccp = new SCCP(method.entryBlock);
        return false;
    }
}
