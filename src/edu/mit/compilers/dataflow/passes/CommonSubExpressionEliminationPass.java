package edu.mit.compilers.dataflow.passes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.TemporaryNameGenerator;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.TemporaryName;
import edu.mit.compilers.dataflow.analyses.AvailableExpressions;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.operand.Operand;

public class CommonSubExpressionEliminationPass extends OptimizationPass {
    public CommonSubExpressionEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
        TemporaryNameGenerator.setTempVariableIndexToHighestValue();
    }

    private static void swapOut(InstructionList tacList,
                                Map<Instruction, Integer> tacToPositionInList,
                                Instruction oldCode,
                                Instruction newCode) {
        final var indexOfOldCode = tacToPositionInList.get(oldCode);
        tacList.replaceIfContainsOldCodeAtIndex(indexOfOldCode, oldCode, newCode);
        tacToPositionInList.put(newCode, indexOfOldCode);
    }


    private static void performCSE(Store store,
                                   Operand operand,
                                   InstructionList tacList,
                                   Set<AbstractName> globalVariables,
                                   HashMap<Operand, AbstractName> expressionToVariable,
                                   HashMap<Instruction, Integer> tacToPositionInList
    ) {
        if (expressionToVariable.containsKey(operand)) {
            // this computation has been made before, so swap it with the variable that already stores the value
            var replacer = Assign.ofRegularAssign(store.getStore(), expressionToVariable.get(operand));
            var indexOfOldCode = tacToPositionInList.get(store);
            /* we check if the oldCode is indeed present in the tac list.
               if it is not, this method throws an IllegalArgumentException
            */
            tacList.replaceIfContainsOldCodeAtIndex(indexOfOldCode, store, replacer);
            tacToPositionInList.put(replacer, indexOfOldCode);
        }
        // the operand already doesn't contain any array name
        if (!operand.containsAny(globalVariables)) {
            expressionToVariable.put(operand, store.getStore());
        }
    }

    private static void discardKilledExpressions(Store store,
                                                 HashMap<Operand, AbstractName> expressionToVariable) {
        // we iterate through a copy of the keys to prevent a ConcurrentCoModificationException
        for (Operand operand : new ArrayList<>(expressionToVariable.keySet())) {
            if (operand.contains(store.getStore())) {
                expressionToVariable.remove(operand);
            }
        }
    }

    /**
     * Maps each tac to its index in its corresponding tac list
     * @param instructionList the subject tac list
     *
     * @return a mapping from tac to its index
     */

    public static HashMap<Instruction, Integer> getTacToPosMapping(InstructionList instructionList) {
        var tacToPositionInList = new HashMap<Instruction, Integer>();
        var index = 0;
        for (Instruction tac : instructionList) {
            tacToPositionInList.put(tac, index);
            ++index;
        }
        return tacToPositionInList;
    }

    public static void performLocalCSE(BasicBlock basicBlock, Set<AbstractName> globalVariables) {
        // this maps each expression to its variable, for instance "a + b" -> c
        // while "a" and "b" have not been re-assigned, all occurrences of "a + b" are replaced with c
        final var expressionToVariable = new HashMap<Operand, AbstractName>();
        // this maps each three address code to it's position in its corresponding TAC list
        // it is helpful when swapping out expressions with their replacements
        final var tacToPositionInList = getTacToPosMapping(basicBlock.instructionList);

        final var tacList = basicBlock.instructionList;

        for (Store store : basicBlock.getStores()) {
            // we don't cache expressions involving array variables
            if (store instanceof BinaryInstruction || store instanceof UnaryInstruction) {
                store.getOperandNoArray()
                        .ifPresent(
                                operand -> performCSE(store, operand, tacList, globalVariables, expressionToVariable, tacToPositionInList));
            }
            // remove all expressions which have been killed by this assignment
            discardKilledExpressions(store, expressionToVariable);
        }
    }


    public void performGlobalCSE() {
        var availableExpressions = new AvailableExpressions(entryBlock);

        // we first perform local CSE for each basic block
        for (var basicBlock : basicBlocks)
            performLocalCSE(basicBlock, globalVariables);

        // we then get the available expressions for each basic block in the CFG
        var availableExpressionsIn = availableExpressions.in;

        for (BasicBlock basicBlock : basicBlocks) {
            final var availableExpressionsForBlock = availableExpressionsIn.get(basicBlock);
            final var tacList = basicBlock.instructionList;
            final var tacToPositionInList = DataFlowAnalysis.getTacToPosMapping(tacList);

            Objects.requireNonNull(availableExpressions, () -> "In[B] for basicBlock " + basicBlock + " not found");

            for (Operand computation : availableExpressionsForBlock) {
                for (Store store : basicBlock.getStores()) {
                    if (store instanceof UnaryInstruction || store instanceof BinaryInstruction) {
                        if (computation.isContainedIn(store) && !isReassignedBeforeUse(basicBlock, computation, store)) {
                            final var operand = backPropagateToEliminateCSEFromCFG(basicBlock, computation, store);
                            final var assign = Assign.ofRegularAssign(store.getStore(), operand);
                            swapOut(tacList, tacToPositionInList, store, assign);
                            break;
                        }
                        if (computation.contains(store.getStore())) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /*
    Search backward from the first occurrence to determine whether any of the operands of exp have been previously assigned to in the block.
    If so, this occurrence of `exp` is not a global common sub-expression;
    proceed to another expression or another block as appropriate.
    */

    private static boolean isReassignedBeforeUse(BasicBlock basicBlock, Operand operand, Store original) {
        var indexOfOriginal = basicBlock.instructionList.indexOf(original);
        assert indexOfOriginal != -1;
        if (indexOfOriginal == 0)
            return false;
        for (int indexOfCode = indexOfOriginal - 1;  indexOfCode >= 0; indexOfCode--) {
            Instruction instruction = basicBlock.instructionList.get(indexOfCode);
            if (instruction instanceof Store) {
                if (operand.contains(((Store) instruction).getStore()) && !isTrivialAssignment(instruction)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AbstractName backPropagateToEliminateCSEFromCFG(BasicBlock basicBlock,
                                                                   Operand operand,
                                                                   Store original) {
        // perform a breadth first search to perform the replacement
        AssignableName uniqueName = null;
        final var queue = new ArrayDeque<>(basicBlock.getPredecessors());
        final var visited = new HashSet<>();

        visited.add(basicBlock);
        while (!queue.isEmpty()) {
            final var current = queue.pop();
            if (visited.contains(current))
                continue;
            visited.add(current);

            var computationFound = false;
            final var updateTacList = new ArrayList<Instruction>();

            final var tacListReversed = current.getCopyOfInstructionList();
            Collections.reverse(tacListReversed);
            for (var threeAddressCode : tacListReversed) {
                if (!computationFound) {
                    if (threeAddressCode instanceof UnaryInstruction || threeAddressCode instanceof BinaryInstruction) {
                        final var hasResult = (Store) threeAddressCode;
                        if (operand.isContainedIn(hasResult)) {
                            // create a new temp
                            uniqueName = TemporaryName.generateTemporaryName(hasResult.getStore().builtinType);
                            updateTacList.add(Assign.ofRegularAssign(hasResult.getStore(), uniqueName));
                            updateTacList.add(operand.storeInstructionFromOperand(uniqueName));
                            computationFound = true;
                            continue;
                        }
                    }
                }
                updateTacList.add(threeAddressCode);
            }
            Collections.reverse(updateTacList);
            final var codes = current.instructionList;
            codes.clear();
            codes.addAll(updateTacList);

            if (!computationFound) {
                queue.addAll(current.getPredecessors());
            }
        }
        Objects.requireNonNull(uniqueName, "uniqueName is null");
        return uniqueName;
    }

    @Override
    public boolean run() {
        var oldTacList = entryBlock.getCopyOfInstructionList();
        performGlobalCSE();
        // return whether this blocks threeAddressCodeList has not changed.
        return oldTacList.equals(entryBlock.instructionList);
    }
}