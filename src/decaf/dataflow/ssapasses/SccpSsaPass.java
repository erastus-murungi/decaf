package decaf.dataflow.ssapasses;

import static com.google.common.base.Preconditions.checkState;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import decaf.cfg.BasicBlock;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.HasOperand;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.names.IrIntegerConstant;
import decaf.codegen.names.IrRegister;
import decaf.common.StronglyConnectedComponentsTarjan;
import decaf.dataflow.OptimizationContext;
import decaf.ssa.SSA;

public class SccpSsaPass extends SsaOptimizationPass {
  boolean changesHappened = false;
  @NotNull
  private List<SCCPOptResult> resultList = new ArrayList<>();
  @MonotonicNonNull
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
                copyInstruction.getValue() instanceof IrRegister)) {
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
          for (IrRegister irRegister : hasOperand.getOperandVirtualRegisters()) {
            var latticeValue = latticeValues.getOrDefault(
                irRegister,
                LatticeElement.bottom()
            );
            if (latticeValue.isConstant()) {
              var inst = hasOperand.copy();
              hasOperand.replaceValue(
                  irRegister,
                  new IrIntegerConstant(
                      latticeValue.getValue(),
                      irRegister.getType()
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
        if (basicBlock.getInstructionList().toString().contains("%15.25"))
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
        for (var lValue : phi.getOperandValues()) {
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
