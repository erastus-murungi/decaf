package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.dataflow.operand.BinaryOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnaryOperand;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Performs common sub-expression elimination within a basic block
 */
public class LocalCSE {

    public LocalCSE() {}

    private static void performCSE(HasResult hasResult,
                            Operand operand,
                            ThreeAddressCodeList tacList,
                            ThreeAddressCode oldCode,
                            Set<AbstractName> globalVariables,
                            HashMap<Operand, AbstractName> expressionToVariable,
                            HashMap<ThreeAddressCode, Integer> tacToPositionInList
    ) {

        if (expressionToVariable.containsKey(operand)) {
            // this computation has been made before, so swap it with the variable that already stores the value
            var replacer = new Assign(hasResult.getResultLocation(), DecafScanner.ASSIGN, expressionToVariable.get(operand), hasResult.source, hasResult.getResultLocation() + " = " + expressionToVariable.get(operand));
            var indexOfOldCode = tacToPositionInList.get(oldCode);
            tacList.replaceIfContainsOldCodeAtIndex(indexOfOldCode, oldCode, replacer);
            tacToPositionInList.put(replacer, indexOfOldCode);
        } else if (!operand.containsAny(globalVariables)) {
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

    public static HashMap<ThreeAddressCode, Integer> getTacToPosMapping(ThreeAddressCodeList threeAddressCodeList) {
        var tacToPositionInList = new HashMap<ThreeAddressCode, Integer>();
        var index = 0;
        for (ThreeAddressCode tac: threeAddressCodeList) {
            tacToPositionInList.put(tac, index);
            ++index;
        }
        return tacToPositionInList;
    }

    public static void performLocalCSEMethodBlocks(BasicBlock entryBasicBlock, Set<AbstractName> globalVariables) {
        DataFlowAnalysis.getReversePostOrder(entryBasicBlock).forEach(basicBlock -> performLocalCSE(basicBlock, globalVariables));
    }

    public static void performLocalCSE(BasicBlock basicBlock, Set<AbstractName> globalVariables) {
        // this maps each expression to its variable, for instance "a + b" -> c
        // while "a" and "b" have not been re-assigned, all occurrences of "a + b" are replaced with c
        HashMap<Operand, AbstractName> expressionToVariable = new HashMap<>();
        // this maps each three address code to it's position in its corresponding TAC list
        // it is helpful when swapping out expressions with their replacements
        HashMap<ThreeAddressCode, Integer> tacToPositionInList = getTacToPosMapping(basicBlock.threeAddressCodeList);

        final var tacList = basicBlock.threeAddressCodeList;

        for (ThreeAddressCode tac : tacList) {
            if (tac instanceof Quadruple) {
                var quadruple = (Quadruple) tac;
                // we don't cache expressions involving array variables
                if (!(quadruple.getResultLocation() instanceof ArrayName)
                        && !(quadruple.fstOperand instanceof ArrayName)
                        && !(quadruple.sndOperand instanceof ArrayName)) {
                    final var binaryComputation = new BinaryOperand(quadruple);
                    performCSE(quadruple, binaryComputation, tacList, tac, globalVariables, expressionToVariable, tacToPositionInList);
                    // remove all expressions which have been killed
                    discardKilledExpressions(quadruple, expressionToVariable);
                }
            } else if (tac instanceof Triple) {
                var triple = (Triple) tac;
                // we don't cache expressions involving array variables
                if (!(triple.getResultLocation() instanceof ArrayName) && !(triple.operand instanceof ArrayName)) {
                    final var unaryComputation = new UnaryOperand(triple);
                    performCSE(triple, unaryComputation, tacList, tac, globalVariables, expressionToVariable, tacToPositionInList);
                    // discard all expressions which have been killed
                    discardKilledExpressions(triple, expressionToVariable);
                }
            } else if (tac instanceof HasResult) {
                // every other assignment kills available expressions
                var hasResult = (HasResult) tac;
                if (!(hasResult.getResultLocation() instanceof ArrayName))
                    discardKilledExpressions(hasResult, expressionToVariable);
            }
        }
    }
}
