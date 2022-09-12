package edu.mit.compilers.dataflow.ssapasses;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.VirtualRegister;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.dataflow.OptimizationContext;
import edu.mit.compilers.ssa.SSA;
import edu.mit.compilers.utils.TarjanSCC;

public class SccpSsaPass extends SsaOptimizationPass<Void> {
    boolean changesHappened = false;
    List<SCCPOptResult> resultList = new ArrayList<>();

    public SccpSsaPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    private void substituteVariablesWithConstants(SCCP sccp) {
        var latticeValues = sccp.getLatticeValues();
        for (BasicBlock basicBlock : getBasicBlocksList()) {
            var instructionList = new ArrayList<Instruction>();
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof BinaryInstruction || instruction instanceof UnaryInstruction || (instruction instanceof CopyInstruction copyInstruction && copyInstruction.getValue() instanceof VirtualRegister)) {
                    var dest = ((StoreInstruction) instruction).getDestination();
                    var latticeValue = latticeValues.get(dest);
                    if (latticeValue.isConstant()) {
                        var copyInstruction = CopyInstruction.noAstConstructor(dest, new NumericalConstant(latticeValue.getValue(), dest.getType()));
                        instructionList.add(copyInstruction);
                        resultList.add(new SCCPOptResult(instruction, copyInstruction));
                        changesHappened = true;
                        continue;

                    }

                }
                if (instruction instanceof HasOperand hasOperand) {
                    for (VirtualRegister virtualRegister : hasOperand.getOperandVirtualRegisters()) {
                        var latticeValue = latticeValues.getOrDefault(virtualRegister, LatticeElement.bottom());
                        if (latticeValue.isConstant()) {
                            var inst = hasOperand.copy();
                            hasOperand.replaceValue(virtualRegister, new NumericalConstant(latticeValue.getValue(), virtualRegister.getType()));
                            resultList.add(new SCCPOptResult(inst, hasOperand));
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

    private void removeUnreachableBasicBlocks(SCCP sccp) {
        for (var basicBlock : getBasicBlocksList()) {
            if (basicBlock.hasBranch()) {
                if (!sccp.isReachable(basicBlock.getTrueTarget()) && !sccp.isReachable(basicBlock.getFalseTarget())) {
                    basicBlock.convertToBranchLess(method.exitBlock);
                    changesHappened = true;
                } else if ((basicBlock.getConditionalBranchInstruction()
                                      .getCondition() instanceof NumericalConstant numericalConstant && numericalConstant.getValue() == 0L)) {
                    basicBlock.convertToBranchLessSkipTrue();
                    changesHappened = true;
                } else if (!sccp.isReachable(basicBlock.getFalseTarget()) ||
                        (basicBlock.getConditionalBranchInstruction()
                                   .getCondition() instanceof NumericalConstant numericalConstant && numericalConstant.getValue() == 1L)) {
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

    private void removePhisFromUnreachableBlocks(SCCP sccp) {
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
        optimizationContext.setBasicBlocks(method, TarjanSCC.getReversePostOrder(method.entryBlock));
    }


    @Override
    public boolean runFunctionPass() {
        var sccp = new SCCP(method);
        changesHappened = false;
        resultList.clear();
        substituteVariablesWithConstants(sccp);
        removeUnreachableBasicBlocks(sccp);
        removePhisFromUnreachableBlocks(sccp);
        SSA.verify(method);
        return changesHappened;
    }
}
