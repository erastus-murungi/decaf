package edu.mit.compilers.dataflow.passes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.dataflow.OptimizationContext;

public class PeepHoleOptimizationPass extends OptimizationPass {
    public PeepHoleOptimizationPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    private void removeTrivialBoundsChecks() {
        for (var basicBlock : getBasicBlocksList()) {
            var indicesToRemove = new ArrayList<Integer>();
            for (var indexOfInstruction = 0; indexOfInstruction < basicBlock.getInstructionList().size(); indexOfInstruction++) {
                var instruction = basicBlock.getInstructionList().get(indexOfInstruction);
                if (instruction instanceof ArrayBoundsCheck arrayBoundsCheck) {
                    if (arrayBoundsCheck.getAddress.getIndex() instanceof NumericalConstant) {
                        var index = Long.parseLong(arrayBoundsCheck.getAddress.getIndex().getLabel());
                        var length = Long.parseLong(arrayBoundsCheck.getAddress.getLength()
                                .orElseThrow().getLabel());
                        if (index >= 0 && index < length) {
                            indicesToRemove.add(indexOfInstruction);
                        }
                    }
                }
            }

            for (var index : indicesToRemove) {
                basicBlock.getInstructionList()
                        .set(index, null);
            }
            basicBlock.getInstructionList().reset(basicBlock.getCopyOfInstructionList()
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
    }


    private void removeEmptyBasicBlocks() {
        for (BasicBlock basicBlock: getBasicBlocksList()) {
            if (!(basicBlock instanceof NOP) && basicBlock.getInstructionList().isEmpty()) {
                checkState(basicBlock.hasNoBranch());
                var replacer = basicBlock.getSuccessor();
                checkNotNull(replacer);
                for (var pred: basicBlock.getPredecessors()) {
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
                }

                for (var successor: basicBlock.getSuccessors()) {
                    successor.removePredecessor(basicBlock);
                }
                basicBlock.clearPredecessors();
            }
        }
        optimizationContext.setBasicBlocks(method, getBasicBlocksList());
    }


    @Override
    public boolean runFunctionPass() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        removeTrivialBoundsChecks();
        removeEmptyBasicBlocks();
        return !oldCodes.equals(entryBlock.getInstructionList());
    }
}
