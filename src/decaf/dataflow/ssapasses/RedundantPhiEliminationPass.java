package decaf.dataflow.ssapasses;

import java.util.ArrayList;

import decaf.codegen.codes.Instruction;
import decaf.common.SSAEdgesUtil;
import decaf.ssa.SSA;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.Method;
import decaf.codegen.names.IrRegister;
import decaf.dataflow.OptimizationContext;
import decaf.ssa.Phi;


/**
 * The redundant PHI instruction is defined as follows:
 * 1) x=phi(x,x,x) (remove only)
 * 2) x=phi(y,y,y) (regard as `x=y` and do copy propagation)
 * 3) x=phi(y,y,x) (regard as `x=y` and do copy propagation)
 */
public class RedundantPhiEliminationPass extends SsaOptimizationPass {
  public RedundantPhiEliminationPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private boolean performRedundantPhiElimination() {
    SSAEdgesUtil ssaEdgesUtil = new SSAEdgesUtil(method);
    var changesHappened = false;
    for (var basicBlock : getBasicBlocksList()) {
      if (basicBlock.phiPresent()) {
        var instructionList = new ArrayList<Instruction>();
        for (var instruction : basicBlock.getInstructionList()) {
          if (instruction instanceof Phi phi) {
            // x=phi(x,x,x) (remove only)
            if (phi.getOperandValues()
                   .stream()
                   .allMatch(lValue -> lValue.equals(phi.getDestination()))) {
              changesHappened = true;
              continue;
            }
            // 2) x=phi(y,y,y) (regard as `x=y` and do copy propagation)
            if (phi.getOperandValues()
                   .stream()
                   .distinct()
                   .count() == 1) {
              var y = phi.getOperandValues()
                         .stream()
                         .findFirst()
                         .orElseThrow();
              instructionList.add(CopyInstruction.noMetaData(
                  phi.getDestination(),
                  y
              ));
              ssaEdgesUtil.copyPropagate(
                  (IrRegister) phi.getDestination(),
                  y
              );
              changesHappened = true;
              continue;
            }
            // 3) x=phi(y,y,x) (regard as `x=y` and do copy propagation)
            if (phi.getOperandValues()
                   .stream()
                   .filter(value -> value.equals(phi.getDestination()))
                   .count() == 1) {
              var y = phi.getOperandValues()
                         .stream()
                         .filter(value -> !value.equals(phi.getDestination()))
                         .findFirst()
                         .orElseThrow();
              instructionList.add(CopyInstruction.noMetaData(
                  phi.getDestination(),
                  y
              ));
              ssaEdgesUtil.copyPropagate(
                  (IrRegister) phi.getDestination(),
                  y
              );
              changesHappened = true;
              continue;
            }
          }
          instructionList.add(instruction);
        }
        basicBlock.getInstructionList()
                  .reset(instructionList);
      }
    }
    return changesHappened;
  }

  @Override
  protected void resetForPass() {
  }

  @Override
  public boolean runFunctionPass() {
    resetForPass();
    var changesHappened = performRedundantPhiElimination();
    SSA.verifySsa(method);
    return changesHappened;
  }
}
