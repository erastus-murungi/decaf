package decaf.ir.dataflow.ssapasses;


import java.util.ArrayList;

import decaf.ir.codes.CopyInstruction;
import decaf.ir.codes.Instruction;
import decaf.ir.codes.Method;
import decaf.ir.names.IrSsaRegister;
import decaf.shared.SSAEdgesUtil;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.ssa.Phi;
import decaf.ir.ssa.SSA;


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
            if (phi.genOperandIrValuesSurface()
                   .stream()
                   .allMatch(lValue -> lValue.equals(phi.getDestination()))) {
              changesHappened = true;
              continue;
            }
            // 2) x=phi(y,y,y) (regard as `x=y` and do copy propagation)
            if (phi.genOperandIrValuesSurface()
                   .stream()
                   .distinct()
                   .count() == 1) {
              var y = phi.genOperandIrValuesSurface()
                         .stream()
                         .findFirst()
                         .orElseThrow();
              instructionList.add(CopyInstruction.noMetaData(
                  phi.getDestination(),
                  y
              ));
              ssaEdgesUtil.copyPropagate(
                  (IrSsaRegister) phi.getDestination(),
                  y
              );
              changesHappened = true;
              continue;
            }
            // 3) x=phi(y,y,x) (regard as `x=y` and do copy propagation)
            if (phi.genOperandIrValuesSurface()
                   .stream()
                   .filter(value -> value.equals(phi.getDestination()))
                   .count() == 1) {
              var y = phi.genOperandIrValuesSurface()
                         .stream()
                         .filter(value -> !value.equals(phi.getDestination()))
                         .findFirst()
                         .orElseThrow();
              instructionList.add(CopyInstruction.noMetaData(
                  phi.getDestination(),
                  y
              ));
              ssaEdgesUtil.copyPropagate(
                  (IrSsaRegister) phi.getDestination(),
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
