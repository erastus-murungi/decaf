package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.analyses.AvailableExpressions;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.dominator.ImmediateDominator;
import edu.mit.compilers.dataflow.operand.Operand;

public class CommonSubExpressionEliminationPass extends OptimizationPass {
    boolean changeHappened = false;

    public CommonSubExpressionEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    private void swapOut(InstructionList instructionList,
                         Map<Instruction, Integer> instructionToIndexMapping,
                         Instruction oldInstruction,
                         Instruction newInstruction) {
        final var indexOfOldInstruction = instructionToIndexMapping.get(oldInstruction);
        instructionList.replaceIfContainsInstuctionAtIndex(indexOfOldInstruction, oldInstruction, newInstruction);
        instructionToIndexMapping.put(newInstruction, indexOfOldInstruction);
        changeHappened = true;
    }


    private void performCSE(Store store,
                            Operand operand,
                            InstructionList instructionList,
                            Set<AbstractName> globalVariables,
                            HashMap<Operand, AbstractName> expressionToVariable,
                            HashMap<Instruction, Integer> instructionToPositionInInstructionList) {

        if (expressionToVariable.containsKey(operand)) {
            // this computation has been made before, so swap it with the variable that already stores the value
            var replacer = Assign.ofRegularAssign(store.getStore(), expressionToVariable.get(operand));
            var indexOfOldCode = instructionToPositionInInstructionList.get(store);
            /* we check if the oldCode is indeed present in the tac list.
               if it is not, this method throws an IllegalArgumentException
            */
            instructionList.replaceIfContainsInstuctionAtIndex(indexOfOldCode, store, replacer);
            instructionToPositionInInstructionList.put(replacer, indexOfOldCode);
            changeHappened = true;
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
     *
     * @param instructionList the subject tac list
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

    public void performLocalCSE(BasicBlock basicBlock, Set<AbstractName> globalVariables) {
        // this maps each expression to its variable, for instance "a + b" -> c
        // while "a" and "b" have not been re-assigned, all occurrences of "a + b" are replaced with c
        final var expressionToVariable = new HashMap<Operand, AbstractName>();
        // this maps each three address code to it's position in its corresponding TAC list
        // it is helpful when swapping out expressions with their replacements
        final var tacToPositionInList = getTacToPosMapping(basicBlock.getInstructionList());

        final var tacList = basicBlock.getInstructionList();

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
        var dom = new ImmediateDominator(this.entryBlock);

        // we first perform local CSE for each basic block
        for (var basicBlock : basicBlocks)
            performLocalCSE(basicBlock, globalVariables);

        // we then get the available expressions for each basic block in the CFG
        var availableExpressionsIn = availableExpressions.in;

        for (BasicBlock basicBlock : basicBlocks) {
            final var availableExpressionsForBlock = availableExpressionsIn.get(basicBlock);
            final var instructionToIndexMapping = DataFlowAnalysis.getInstructionToIndexMapping(basicBlock.getInstructionList());

            Objects.requireNonNull(availableExpressions, () -> "In[B] for basicBlock " + basicBlock + " not found");

            for (Operand availableExpression : availableExpressionsForBlock) {
                // walk up a nodes dominators and replace this expression with the earliest instance of the store
                for (Store store : basicBlock.getStores()) {
                    if (store instanceof UnaryInstruction || store instanceof BinaryInstruction) {
                        if (availableExpression.isContainedIn(store) && !isReassignedBeforeUse(basicBlock, availableExpression, store)) {
                            final var expressionName = findExpressionAmongDominators(basicBlock, availableExpression, dom);
                            final var assign = Assign.ofRegularAssign(store.getStore(), expressionName);
                            swapOut(basicBlock.getInstructionList(), instructionToIndexMapping, store, assign);
                            break;
                        }
                        if (availableExpression.contains(store.getStore())) {
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
        var indexOfOriginal = basicBlock.getInstructionList()
                                        .indexOf(original);
        assert indexOfOriginal != -1;
        if (indexOfOriginal == 0)
            return false;
        for (int indexOfCode = indexOfOriginal - 1; indexOfCode >= 0; indexOfCode--) {
            Instruction instruction = basicBlock.getInstructionList()
                                                .get(indexOfCode);
            if (instruction instanceof Store) {
                Store storeInstruction = ((Store) instruction);
                if (operand.contains(storeInstruction.getStore()) && !isTrivialAssignment(instruction)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AbstractName findExpressionAmongDominators(BasicBlock B,
                                                              Operand operand,
                                                              ImmediateDominator dominatorTree) {
        for (BasicBlock dominator : dominatorTree.getDominators(B)) {
            for (Instruction instruction : dominator.getInstructionList()) {
                if (instruction instanceof BinaryInstruction || instruction instanceof UnaryInstruction) {
                    final var storeInstruction = (Store) instruction;
                    if (operand.isContainedIn(storeInstruction)) {
                        return storeInstruction.getStore();
                    }
                }
            }
        }
        // find first block which dominates B and doesn't
        throw new IllegalStateException("expression not found");
    }

    @Override
    public boolean run() {
        changeHappened = false;
        performGlobalCSE();
        return changeHappened;
    }
}