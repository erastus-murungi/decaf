package edu.mit.compilers.dataflow.passes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.dataflow.OptimizationContext;
import edu.mit.compilers.ssa.Phi;

public class PeepHoleOptimizationPass extends OptimizationPass {
  boolean changesHappened = false;

  public PeepHoleOptimizationPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(optimizationContext,
        method);
  }


  private void removeEmptyBasicBlocks() {
    for (BasicBlock basicBlock : getBasicBlocksList()) {
      if (!(basicBlock instanceof NOP) && basicBlock.getInstructionList()
                                                    .isEmpty()) {
        checkState(basicBlock.hasNoBranch());
        var replacer = basicBlock.getSuccessor();
        checkNotNull(replacer);
        var predecessors = new ArrayList<>(basicBlock.getPredecessors());
        for (var pred : predecessors) {
          if (pred.hasBranch()) {
            checkState(basicBlock.getSuccessor() != null);
            if (basicBlock == pred.getTrueTarget()) {
              pred.setTrueTarget(replacer);
            } else {
              checkState(pred.getFalseTarget() == basicBlock);
              pred.setFalseTargetUnchecked(replacer);
            }
          } else {
            pred.setSuccessor(replacer);
          }
          replacer.addPredecessor(pred);
          basicBlock.getTributaries()
                    .forEach(withTarget -> withTarget.replaceTarget(replacer));
          changesHappened = true;
        }
        if (replacer.phiPresent()) {
          replacer.getPhiFunctions()
                  .forEach(phi -> phi.replaceBlock(basicBlock));
        }

        for (var successor : basicBlock.getSuccessors()) {
          successor.removePredecessor(basicBlock);
        }
        basicBlock.clearPredecessors();
      }
    }
    optimizationContext.setBasicBlocks(method,
        getBasicBlocksList());
  }


  @Override
  public boolean runFunctionPass() {
    changesHappened = false;
    removeEmptyBasicBlocks();
    return changesHappened;
  }
}
