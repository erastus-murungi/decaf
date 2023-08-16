package decaf.ir.dataflow.ssapasses;

import java.util.ArrayList;
import java.util.List;

import decaf.ir.codes.Method;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.dataflow.dominator.DominatorTree;

public class LoopAnalysisPass extends SsaOptimizationPass {
  private DominatorTree dominatorTree;

  public LoopAnalysisPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private List<NaturalLoop> findNaturalLoops(DominatorTree dominatorTree) {
    var naturalLoops = new ArrayList<NaturalLoop>();

    for (var node : getBasicBlocksList()) {
      for (var successor : node.getSuccessors()) {
        if (dominatorTree.dom(
            successor,
            node
        )) {
          naturalLoops.add(new NaturalLoop(
              successor,
              node,
              dominatorTree
          ));
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
