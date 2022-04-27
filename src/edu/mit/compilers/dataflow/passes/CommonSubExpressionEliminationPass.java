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
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.codes.Triple;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.analyses.AvailableExpressions;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.grammar.DecafScanner;

public class CommonSubExpressionEliminationPass extends OptimizationPass {
    public CommonSubExpressionEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    private static void swapOut(ThreeAddressCodeList tacList,
                                Map<ThreeAddressCode, Integer> tacToPositionInList,
                                ThreeAddressCode oldCode,
                                ThreeAddressCode newCode) {
        final var indexOfOldCode = tacToPositionInList.get(oldCode);
        tacList.replaceIfContainsOldCodeAtIndex(indexOfOldCode, oldCode, newCode);
        tacToPositionInList.put(newCode, indexOfOldCode);
    }


    private static void performCSE(HasResult hasResult,
                                   Operand operand,
                                   ThreeAddressCodeList tacList,
                                   Set<AbstractName> globalVariables,
                                   HashMap<Operand, AbstractName> expressionToVariable,
                                   HashMap<ThreeAddressCode, Integer> tacToPositionInList
    ) {
        if (expressionToVariable.containsKey(operand)) {
            // this computation has been made before, so swap it with the variable that already stores the value
            var replacer = Assign.ofRegularAssign(hasResult.getResultLocation(), expressionToVariable.get(operand));
            var indexOfOldCode = tacToPositionInList.get(hasResult);
            /* we check if the oldCode is indeed present in the tac list.
               if it is not, this method throws an IllegalArgumentException
            */
            tacList.replaceIfContainsOldCodeAtIndex(indexOfOldCode, hasResult, replacer);
            tacToPositionInList.put(replacer, indexOfOldCode);
        }
        // the operand already doesn't contain any array name
        if (!operand.containsAny(globalVariables)) {
            expressionToVariable.put(operand, hasResult.getResultLocation());
        }
    }

    private static void discardKilledExpressions(HasResult hasResult,
                                                 HashMap<Operand, AbstractName> expressionToVariable) {
        // we iterate through a copy of the keys to prevent a ConcurrentCoModificationException
        for (Operand operand : new ArrayList<>(expressionToVariable.keySet())) {
            if (operand.contains(hasResult.getResultLocation())) {
                expressionToVariable.remove(operand);
            }
        }
    }

    /**
     * Maps each tac to its index in its corresponding tac list
     * @param threeAddressCodeList the subject tac list
     *
     * @return a mapping from tac to its index
     */

    public static HashMap<ThreeAddressCode, Integer> getTacToPosMapping(ThreeAddressCodeList threeAddressCodeList) {
        var tacToPositionInList = new HashMap<ThreeAddressCode, Integer>();
        var index = 0;
        for (ThreeAddressCode tac : threeAddressCodeList) {
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
        final var tacToPositionInList = getTacToPosMapping(basicBlock.threeAddressCodeList);

        final var tacList = basicBlock.threeAddressCodeList;

        for (HasResult hasResult : basicBlock.assignments()) {
            // we don't cache expressions involving array variables
            if (hasResult instanceof Quadruple || hasResult instanceof Triple) {
                hasResult.getComputationNoArray()
                        .ifPresent(
                                operand -> performCSE(hasResult, operand, tacList, globalVariables, expressionToVariable, tacToPositionInList));
            }
            // remove all expressions which have been killed by this assignment
            discardKilledExpressions(hasResult, expressionToVariable);
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
            final var tacList = basicBlock.threeAddressCodeList;
            final var tacToPositionInList = DataFlowAnalysis.getTacToPosMapping(tacList);

            Objects.requireNonNull(availableExpressions, () -> "In[B] for basicBlock " + basicBlock + " not found");

            for (Operand computation : availableExpressionsForBlock) {
                for (HasResult hasResult : basicBlock.assignments()) {
                    if (hasResult instanceof Triple || hasResult instanceof Quadruple) {
                        if (computation.isContainedIn(hasResult) && !isReassignedBeforeUse(basicBlock, computation, hasResult)) {
                            final var operand = backPropagateToEliminateCSEFromCFG(basicBlock, computation, hasResult);
                            final var assign = Assign.ofRegularAssign(hasResult.getResultLocation(), operand);
                            swapOut(tacList, tacToPositionInList, hasResult, assign);
                            break;
                        }
                        if (computation.contains(hasResult.getResultLocation())) {
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

    private static boolean isReassignedBeforeUse(BasicBlock basicBlock, Operand operand, HasResult original) {
        var indexOfOriginal = basicBlock.threeAddressCodeList.getCodes().indexOf(original);
        assert indexOfOriginal != -1;
        if (indexOfOriginal == 0)
            return false;
        for (int indexOfCode = indexOfOriginal - 1;  indexOfCode >= 0; indexOfCode--) {
            ThreeAddressCode threeAddressCode = basicBlock.threeAddressCodeList.get(indexOfCode);
            if (threeAddressCode instanceof HasResult) {
                if (operand.contains(((HasResult) threeAddressCode).getResultLocation()) && !isTrivialAssignment(threeAddressCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    // return whether an instruction of the form x = x
    private static boolean isTrivialAssignment(ThreeAddressCode threeAddressCode) {
        if (threeAddressCode instanceof Assign) {
            var assign = (Assign) threeAddressCode;
            if (assign.assignmentOperator.equals(DecafScanner.ASSIGN)) {
                return assign.dst.equals(assign.operand);
            }
        }
        return false;
    }

    private static AbstractName backPropagateToEliminateCSEFromCFG(BasicBlock basicBlock,
                                                                   Operand operand,
                                                                   HasResult original) {
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
            final var updateTacList = new ArrayList<ThreeAddressCode>();

            final var tacListReversed = current.codes();
            Collections.reverse(tacListReversed);
            for (var threeAddressCode : tacListReversed) {
                if (!computationFound) {
                    if (threeAddressCode instanceof Triple || threeAddressCode instanceof Quadruple) {
                        final var hasResult = (HasResult) threeAddressCode;
                        if (operand.isContainedIn(hasResult)) {
                            uniqueName = hasResult.getResultLocation();
                            computationFound = true;
                        }
                    }
                }
                updateTacList.add(threeAddressCode);
            }
            Collections.reverse(updateTacList);
            final var codes = current.threeAddressCodeList.getCodes();
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
        var oldTacList = entryBlock.codes();
        performGlobalCSE();
        // return whether this blocks threeAddressCodeList has not changed.
        return oldTacList.equals(entryBlock.threeAddressCodeList.getCodes());
    }
}