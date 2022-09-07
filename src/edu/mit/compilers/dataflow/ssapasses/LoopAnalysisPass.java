package edu.mit.compilers.dataflow.ssapasses;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.dataflow.OptimizationContext;
import edu.mit.compilers.dataflow.dominator.DominatorTree;

public class LoopAnalysisPass extends SsaOptimizationPass<Void> {
    public LoopAnalysisPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    private List<NaturalLoop> findNaturalLoops(DominatorTree dominatorTree) {
        var naturalLoops = new ArrayList<NaturalLoop>();

        for (var node : getBasicBlocksList()) {
            for (var successor : node.getSuccessors()) {
                if (dominatorTree.dom(successor, node)) {
                    naturalLoops.add(new NaturalLoop(successor, node, dominatorTree));
                }
            }
        }

        return naturalLoops;
    }

    @Override
    public boolean runFunctionPass() {
        var dominatorTree = new DominatorTree(method.entryBlock);
        var naturalLoops = findNaturalLoops(dominatorTree);
        return false;
    }
}
