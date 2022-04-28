package edu.mit.compilers.dataflow.passes;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.ArrayAccess;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.MethodCallNoResult;
import edu.mit.compilers.codegen.codes.MethodCallSetResult;
import edu.mit.compilers.codegen.codes.PushParameter;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.StringConstantName;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.dataflow.operand.AugmentedOperand;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DeadCodeEliminationPass extends OptimizationPass {
    public DeadCodeEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    // return whether an instruction of the form x = x
    private boolean isTrivialAssignment(ThreeAddressCode threeAddressCode) {
        if (threeAddressCode instanceof Assign) {
            var assign = (Assign) threeAddressCode;
            if (assign.assignmentOperator.equals(DecafScanner.ASSIGN)) {
                return assign.dst.equals(assign.operand);
            }
        }
        return false;
    }

    private boolean augmentedAssignVariablesNeeded(ThreeAddressCode tac) {
        if (tac instanceof HasOperand) {
            var hasOperand = (HasOperand) tac;
            var operand = hasOperand.getOperand();
            if (operand instanceof AugmentedOperand) {
                return hasOperand.getOperandNamesNoArray()
                        .stream()
                        .allMatch(this::allConstants);
            }
        }
        return false;

    }

    private boolean allConstants(AbstractName abstractName) {
        return abstractName instanceof ConstantName || abstractName instanceof StringConstantName;
    }

    private boolean resultVariableIsGlobal(ThreeAddressCode threeAddressCode) {
        if (threeAddressCode instanceof HasResult)
            return globalVariables.contains(((HasResult) threeAddressCode).dst);
        return false;
    }

    private boolean eachNameIsArrayOrGlobalOrInNeededSet(ThreeAddressCode threeAddressCode, Set<AbstractName> neededSet) {
        for (AbstractName abstractName: threeAddressCode.getNames()) {
            if (!globalVariables.contains(abstractName) || !(abstractName instanceof ArrayName) || !neededSet.contains(abstractName)) {
                return false;
            }
        }
        return true;
    }

    private boolean allVariablesInNeededSet(Set<AbstractName> neededSet, ThreeAddressCode tac) {
        var allOperandsAreConstants = tac.getNames()
                .stream()
                .allMatch(this::allConstants);
        return eachNameIsArrayOrGlobalOrInNeededSet(tac, neededSet) ||
                augmentedAssignVariablesNeeded(tac) ||
                allOperandsAreConstants;
    }

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
        final var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);

        for (var basicBlock : basicBlocks) {

            final var usedLaterGlobal = Collections.unmodifiableSet(liveVariableAnalysis.usedLater(basicBlock));
            final var neededSet = new HashSet<>(usedLaterGlobal);

            // if all the variables used in this instruction are not in the needed set, then ignore
            // this instruction while reconstructing this basicBlocks TAC list
            // note that we ignore global variables and array names here
            // remove useless assignments like x = x
            var newTacList = new ArrayList<ThreeAddressCode>();
            var reversedTacList = basicBlock.codes();
            Collections.reverse(reversedTacList);
            for (var tac: reversedTacList) {
                if (tac instanceof HasOperand) {
                    // add operands to neededSet
                    var hasOperand = ((HasOperand) tac);
                    neededSet.addAll(hasOperand.getOperandNamesNoArray());
                }
                if (tac instanceof HasResult) {
                    var hasResult = ((HasResult) tac);
                    var location = hasResult.getResultLocation();
                    if (!(location instanceof ArrayName) && !(neededSet.contains(location))) {
                        continue;
                    }
                }
                if (isTrivialAssignment(tac))
                    continue;

                newTacList.add(tac);
            }
            Collections.reverse(newTacList);
            basicBlock.threeAddressCodeList.reset(newTacList);

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
