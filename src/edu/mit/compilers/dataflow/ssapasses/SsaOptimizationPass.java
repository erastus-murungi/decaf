package edu.mit.compilers.dataflow.ssapasses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.passes.OptimizationPass;
import edu.mit.compilers.utils.TarjanSCC;

public abstract class SsaOptimizationPass<T> extends OptimizationPass {
    protected Set<LValue> globalVariables;
    protected Method method;
    private final List<BasicBlock> basicBlockList;
    protected final List<OptimizationResult<T>> optimizationResults = new ArrayList<>();


//    Set<Edge> cfgEdges() {
//        var seen = new HashSet<Edge>();
//        for (BasicBlock basicBlock: basicBlockList) {
//            for (BasicBlock successor: basicBlock.getSuccessors()) {
//                seen.add(new Edge(basicBlock, successor));
//            }
//        }
//        return seen;
//    }

    public SsaOptimizationPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
        this.globalVariables = globalVariables;
        this.method = method;
        basicBlockList = TarjanSCC.getReversePostOrder(getEntryBasicBlock());
    }

    protected BasicBlock getEntryBasicBlock() {
        return method.entryBlock;
    }

    protected Collection<BasicBlock> getBasicBlockList() {
        return basicBlockList;
    }

    public List<OptimizationResult<T>> getOptimizationResults() {
        return optimizationResults;
    }

}
