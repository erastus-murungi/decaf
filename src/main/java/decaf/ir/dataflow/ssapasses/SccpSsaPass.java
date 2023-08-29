package decaf.ir.dataflow.ssapasses;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.names.IrIntegerConstant;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.ssa.SSA;
import decaf.shared.StronglyConnectedComponentsTarjan;

public class SccpSsaPass extends SsaOptimizationPass {
  boolean changesHappened = false;

  private List<SCCPOptResult> resultList = new ArrayList<>();
  private SCCP sccp;

  public SccpSsaPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private void substituteVariablesWithConstants() {
    var latticeValues = sccp.getLatticeValues();
    for (BasicBlock basicBlock : getBasicBlocksList()) {
      var instructionList = new ArrayList<Instruction>();
      for (Instruction instruction : basicBlock.getInstructionList()) {
        if (instruction instanceof BinaryInstruction || instruction instanceof UnaryInstruction ||
            (instruction instanceof CopyInstruction copyInstruction &&
                copyInstruction.getValue() instanceof IrSsaRegister)) {
          var dest = ((StoreInstruction) instruction).getDestination();
          var latticeValue = latticeValues.get(dest);
          if (latticeValue.isConstant()) {
            var copyInstruction = CopyInstruction.noAstConstructor(
                dest,
                new IrIntegerConstant(
                    latticeValue.getValue(),
                    dest.getType()
                )
            );
            instructionList.add(copyInstruction);
            resultList.add(new SCCPOptResult(
                instruction,
                copyInstruction
            ));
            changesHappened = true;
            continue;

          }

        }
        if (instruction instanceof HasOperand hasOperand) {
          for (IrSsaRegister irSsaRegister : hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class)) {
            var latticeValue = latticeValues.getOrDefault(
                irSsaRegister,
                LatticeElement.bottom()
            );
            if (latticeValue.isConstant()) {
              var inst = hasOperand.copy();
              hasOperand.replaceValue(
                  irSsaRegister,
                  new IrIntegerConstant(
                      latticeValue.getValue(),
                      irSsaRegister.getType()
                  )
              );
              resultList.add(new SCCPOptResult(
                  inst,
                  hasOperand
              ));
              changesHappened = true;
            }
          }
        }
        instructionList.add(instruction);
      }
      basicBlock.getInstructionList()
                .reset(instructionList);

    }
  }

  private void removeUnreachableBasicBlocks() {
    for (var basicBlock : getBasicBlocksList()) {
      if (basicBlock.hasBranch()) {
        if (basicBlock.getInstructionList()
                      .toString()
                      .contains("%15.25"))
          System.out.println();
        if (!sccp.isReachable(basicBlock.getTrueTarget()) && !sccp.isReachable(basicBlock.getFalseTarget())) {
          basicBlock.convertToBranchLess(method.getExitBlock());
          changesHappened = true;
        } else if ((basicBlock.getConditionalBranchInstruction()
                              .getCondition() instanceof IrIntegerConstant numericalConstant &&
            numericalConstant.getValue() == 0L)) {
          basicBlock.convertToBranchLessSkipTrue();
          changesHappened = true;
        } else if (!sccp.isReachable(basicBlock.getFalseTarget()) || (basicBlock.getConditionalBranchInstruction()
                                                                                .getCondition() instanceof IrIntegerConstant numericalConstant &&
            numericalConstant.getValue() == 1L)) {
          basicBlock.convertToBranchLess(basicBlock.getTrueTarget());
          changesHappened = true;
        } else if (!sccp.isReachable(basicBlock.getTrueTarget())) {
          basicBlock.convertToBranchLess(basicBlock.getFalseTarget());
          changesHappened = true;
        }
      } else if (basicBlock.hasNoBranchNotNOP()) {
        if (!sccp.isReachable(basicBlock)) {
          for (BasicBlock pred : basicBlock.getPredecessors()) {
            if (basicBlock == pred.getSuccessor()) {
              pred.setSuccessor(basicBlock.getSuccessor());
              changesHappened = true;
            } else {
              if (pred.hasBranch()) {
                checkState(basicBlock == pred.getAlternateSuccessor());
                checkState(pred.hasBranch());
                pred.setFalseTarget(basicBlock);
                changesHappened = true;
              }
            }
          }
          for (BasicBlock successor : basicBlock.getSuccessors()) {
            successor.removePredecessor(basicBlock);
          }
        }
      }
    }
  }

  private void removePhisFromUnreachableBlocks() {
    for (var basicBlock : getBasicBlocksList()) {
      for (var phi : basicBlock.getPhiFunctions()) {
        for (var lValue : phi.genOperandIrValuesSurface()) {
          var owner = phi.getBasicBlockForV(lValue);
          if (!(sccp.isReachable(owner))) {
            phi.removePhiOperand(lValue);
            changesHappened = true;
          }
        }
      }
    }
    optimizationContext.setBasicBlocks(
        method,
        StronglyConnectedComponentsTarjan.getReversePostOrder(method.getEntryBlock())
    );
  }

  @Override
  protected void resetForPass() {
    sccp = new SCCP(method);
    resultList = new ArrayList<>();
    changesHappened = false;
  }


  @Override
  public boolean runFunctionPass() {
    resetForPass();
    substituteVariablesWithConstants();
    removeUnreachableBasicBlocks();
    removePhisFromUnreachableBlocks();
    SSA.verifySsa(method);
    return changesHappened;
  }
}
