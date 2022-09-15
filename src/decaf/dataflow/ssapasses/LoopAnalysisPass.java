package decaf.dataflow.ssapasses;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.ArrayList;
import java.util.List;

import decaf.dataflow.OptimizationContext;
import decaf.codegen.codes.Method;
import decaf.dataflow.dominator.DominatorTree;

public class LoopAnalysisPass extends SsaOptimizationPass {
    @MonotonicNonNull private DominatorTree dominatorTree;

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
    protected void resetForPass() {
        dominatorTree = new DominatorTree(method.getEntryBlock());
    }

    @Override
    public boolean runFunctionPass() {
        resetForPass();
        var naturalLoops = findNaturalLoops(dominatorTree);
        return false;
    }
}
