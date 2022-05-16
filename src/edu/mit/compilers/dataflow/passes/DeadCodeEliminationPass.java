package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.FunctionCall;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.PushArgument;
import edu.mit.compilers.codegen.names.AbstractName;

public class DeadCodeEliminationPass extends OptimizationPass {
    public DeadCodeEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    /**
     * removes push's which are more than needed for function call
     */
    private boolean removePushParametersExcessPushes(BasicBlock basicBlock) {
        // remove all push parameters which don't have a method call afterwards
        var indicesToRemove = new ArrayList<Integer>();
        boolean changed = false;
        for (int indexOfCode = 0; indexOfCode < basicBlock.instructionList.size(); indexOfCode++) {
            var tac = basicBlock.instructionList.get(indexOfCode);
            if (tac instanceof FunctionCall) {
                int numberOfArguments = ((FunctionCall) tac).numberOfArguments();
                int indexToStartCheck = indexOfCode - numberOfArguments - 1;
                while (indexToStartCheck >= 0 && basicBlock.instructionList.get(indexToStartCheck) instanceof PushArgument) {
                    indicesToRemove.add(indexToStartCheck);
                    indexToStartCheck--;
                    changed = true;
                }
            }
        }
        for (var index : indicesToRemove) {
            basicBlock.instructionList
                    .set(index, null);
        }
        basicBlock.instructionList.reset(basicBlock.getCopyOfInstructionList()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return changed;
    }

    // remove pushes which do not follow a function call
    private void removeDanglingPushes(BasicBlock basicBlock, boolean changed) {
        var indicesToRemove = new ArrayList<Integer>();
        for (int indexOfCode = 0; indexOfCode < basicBlock.instructionList.size(); indexOfCode++) {
            var tac = basicBlock.instructionList.get(indexOfCode);
            if (changed && tac instanceof PushArgument) {
                var prevCode = basicBlock.instructionList.get(indexOfCode - 1);
                var nextCode = basicBlock.instructionList.get(indexOfCode + 1);
                boolean firstCondition = prevCode instanceof MethodBegin || prevCode instanceof PushArgument || prevCode instanceof FunctionCall;
                boolean secondCondition = nextCode instanceof PushArgument || nextCode instanceof FunctionCall;
                if (!(firstCondition && secondCondition)) {
                    indicesToRemove.add(indexOfCode);
                }
            }
        }
        for (var index : indicesToRemove) {
            basicBlock.instructionList.set(index, null);
        }
        basicBlock.instructionList.reset(basicBlock.getCopyOfInstructionList()
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
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        performGlobalDeadCodeElimination();
        return oldCodes.equals(entryBlock.instructionList);
    }
}
