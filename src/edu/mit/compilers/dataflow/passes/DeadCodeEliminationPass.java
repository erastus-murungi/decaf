package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.MethodCallNoResult;
import edu.mit.compilers.codegen.codes.MethodCallSetResult;
import edu.mit.compilers.codegen.codes.PushParameter;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.StringConstantName;
import edu.mit.compilers.dataflow.operand.AugmentedOperand;

public class DeadCodeEliminationPass extends OptimizationPass {
    public DeadCodeEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

//    private boolean augmentedAssignVariablesNeeded(ThreeAddressCode tac) {
//        if (tac instanceof HasOperand) {
//            var hasOperand = (HasOperand) tac;
//            var operand = hasOperand.getOperand();
//            if (operand instanceof AugmentedOperand) {
//                return hasOperand.getOperandNamesNoArray()
//                        .stream()
//                        .allMatch(this::isConstant);
//            }
//        }
//        return false;
//
//    }

    /**
     * removes push's which are more than needed for function call
     */
    private boolean removePushParametersExcessPushes(BasicBlock basicBlock) {
        // remove all push parameters which don't have a method call afterwards
        var indicesToRemove = new ArrayList<Integer>();
        boolean changed = false;
        for (int indexOfCode = 0; indexOfCode < basicBlock.threeAddressCodeList.size(); indexOfCode++) {
            var tac = basicBlock.threeAddressCodeList.get(indexOfCode);
            if (tac instanceof MethodCallSetResult || tac instanceof MethodCallNoResult) {
                int numberOfArguments = (tac instanceof MethodCallNoResult) ? ((MethodCallNoResult) tac).numberOfArguments() : ((MethodCallSetResult) tac).numberOfArguments();
                int indexToStartCheck = indexOfCode - numberOfArguments - 1;
                while (indexToStartCheck >= 0 && basicBlock.threeAddressCodeList.get(indexToStartCheck) instanceof PushParameter) {
                    indicesToRemove.add(indexToStartCheck);
                    indexToStartCheck--;
                    changed = true;
                }
            }
        }
        for (var index : indicesToRemove) {
            basicBlock.threeAddressCodeList.getCodes()
                    .set(index, null);
        }
        basicBlock.threeAddressCodeList.reset(basicBlock.codes()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return changed;
    }

    // remove pushes which do not follow a function call
    private void removeDanglingPushes(BasicBlock basicBlock, boolean changed) {
        var indicesToRemove = new ArrayList<Integer>();
        for (int indexOfCode = 0; indexOfCode < basicBlock.threeAddressCodeList.size(); indexOfCode++) {
            var tac = basicBlock.threeAddressCodeList.get(indexOfCode);
            if (changed && tac instanceof PushParameter) {
                var prevCode = basicBlock.threeAddressCodeList.get(indexOfCode - 1);
                var nextCode = basicBlock.threeAddressCodeList.get(indexOfCode + 1);
                boolean firstCondition = prevCode instanceof MethodBegin || prevCode instanceof PushParameter || prevCode instanceof MethodCallSetResult || prevCode instanceof MethodCallNoResult;
                boolean secondCondition = nextCode instanceof PushParameter || nextCode instanceof MethodCallSetResult || nextCode instanceof MethodCallNoResult;
                if (!(firstCondition && secondCondition)) {
                    indicesToRemove.add(indexOfCode);
                }
            }
        }
        for (var index : indicesToRemove) {
            basicBlock.threeAddressCodeList.getCodes()
                    .set(index, null);
        }
        basicBlock.threeAddressCodeList.reset(basicBlock.codes()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public void performGlobalDeadCodeElimination() {
        for (var basicBlock : basicBlocks) {
            boolean changed = removePushParametersExcessPushes(basicBlock);
            removeDanglingPushes(basicBlock, changed);
        }
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.codes();
        performGlobalDeadCodeElimination();
        return oldCodes.equals(entryBlock.threeAddressCodeList.getCodes());
    }
}
