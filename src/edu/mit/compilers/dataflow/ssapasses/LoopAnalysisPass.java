package edu.mit.compilers.dataflow.ssapasses;

import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.dataflow.dominator.DominatorTree;
import edu.mit.compilers.utils.GraphVizPrinter;

public class LoopAnalysisPass extends SsaOptimizationPass<Void> {
    private static class BackEdge {
        public BasicBlock head, tail;

        public BackEdge(BasicBlock head, BasicBlock tail) {
            this.head = head;
            this.tail = tail;
        }
    }

    public LoopAnalysisPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    private void buildBackEdges(BasicBlock basicBlock, DominatorTree dominatorTree, List<BackEdge> backEdges) {
        for (BasicBlock pred : basicBlock.getPredecessors()) {
            if (dominatorTree.dom(basicBlock, pred)) {
                backEdges.add(new BackEdge(pred, basicBlock));
            }
        }
        for (BasicBlock child : dominatorTree.getChildren(basicBlock)) {
            buildBackEdges(child, dominatorTree, backEdges);
        }
    }

    @Override
    public boolean runFunctionPass() {
        return false;
    }
}
