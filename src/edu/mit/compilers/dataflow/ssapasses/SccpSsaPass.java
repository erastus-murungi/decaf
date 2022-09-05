package edu.mit.compilers.dataflow.ssapasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.ssa.SSA;

public class SccpSsaPass extends SsaOptimizationPass<Void> {
    boolean changesHappened = false;
    List<SCCPOptResult> resultList = new ArrayList<>();

    public SccpSsaPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    private void substituteVariablesWithConstants(SCCP sccp) {
        var latticeValues = sccp.getLatticeValues();
        for (BasicBlock basicBlock : getBasicBlockList()) {
            var instructionList = new ArrayList<Instruction>();
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof BinaryInstruction || instruction instanceof UnaryInstruction || (instruction instanceof CopyInstruction copyInstruction && copyInstruction.getValue() instanceof LValue)) {
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
                    for (LValue lValue : hasOperand.getOperandLValues()) {
                        var latticeValue = latticeValues.getOrDefault(lValue, LatticeElement.bottom());
                        if (latticeValue.isConstant()) {
                            var inst = hasOperand.copy();
                            hasOperand.replaceValue(lValue, new NumericalConstant(latticeValue.getValue(), lValue.getType()));
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
        for (var basicBlock : getBasicBlockList()) {
            if (basicBlock.hasBranch()) {
                if (!sccp.isReachable(basicBlock.getTrueTarget()) && !sccp.isReachable(basicBlock.getFalseTarget())) {
                    basicBlock.convertToBranchLess(method.exitBlock);
                    changesHappened = true;
                } else if (!sccp.isReachable(basicBlock.getFalseTarget())) {
                    basicBlock.convertToBranchLess(basicBlock.getTrueTarget());
                    changesHappened = true;
                } else if (!sccp.isReachable(basicBlock.getTrueTarget())) {
                    basicBlock.convertToBranchLess(basicBlock.getFalseTarget());
                    changesHappened = true;
                }
            }
        }
    }

    private void removePhisFromUnreachableBlocks(SCCP sccp) {
        for (var basicBlock : getBasicBlockList()) {
            for (var phi : basicBlock.getPhiFunctions()) {
                for (var lValue : phi.getOperandValues()) {
                    var owner = phi.getBasicBlockForV(lValue);
                    assert owner != null;
                    if (!(sccp.isReachable(owner))) {
                        phi.removePhiOperand(lValue);
                        changesHappened = true;
                    }
                }
            }
        }
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
