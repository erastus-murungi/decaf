package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.dataflow.operand.BinaryOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnaryOperand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;

import java.util.*;

public class CopyPropagation {
    private static boolean performLocalCopyPropagation(BasicBlock basicBlock, HashMap<AbstractName, Operand> copies) {
        // we need a reference tac list to know whether any changes occurred
        final var oldTacList = basicBlock.codes();
        final var tacList = basicBlock.threeAddressCodeList;
        final var newTacList = basicBlock.threeAddressCodeList.clone();
        newTacList
                .getCodes()
                .clear();

        for (ThreeAddressCode threeAddressCode : tacList) {
            boolean added = false;
            if (threeAddressCode instanceof HasOperand) {
                var hasOperand = (HasOperand) threeAddressCode;
                var targetOperand = hasOperand.getOperand();
                if (targetOperand instanceof UnmodifiedOperand) {
                    var abstractName = ((UnmodifiedOperand) targetOperand).abstractName;
                    if (copies.containsKey(abstractName)) {
                        // do something
                        var replacer = copies.get(abstractName);
                        if (replacer instanceof UnmodifiedOperand)
                            hasOperand.replace(abstractName, ((UnmodifiedOperand) replacer).abstractName);
                        try {
                            HasResult hasResult = (HasResult) threeAddressCode;
                            newTacList.addCode(copies
                                    .get(abstractName)
                                    .fromOperand(hasResult.getResultLocation()));
                            added = true;
                        } catch (ClassCastException ignored) {
                        }
                    }
                } else if (targetOperand instanceof UnaryOperand || targetOperand instanceof BinaryOperand) {
                    for (AbstractName abstractName : hasOperand.getOperandNamesNoArray()) {
                        var replacer = copies.get(abstractName);
                        if (replacer instanceof UnmodifiedOperand) {
                            hasOperand.replace(abstractName, ((UnmodifiedOperand) replacer).abstractName);
                        }
                    }
                }
            }
            if (threeAddressCode instanceof HasResult) {
                var hasResult = (HasResult) threeAddressCode;
                var resultLocation = hasResult.getResultLocation();
                if (!(resultLocation instanceof ArrayName)) {
                    for (AbstractName assignableName : new ArrayList<>(copies.keySet())) {
                        Operand operand = copies.get(assignableName);
                        if (assignableName.equals(resultLocation) ||
                                ((operand instanceof UnmodifiedOperand) && ((UnmodifiedOperand) operand).abstractName.equals(resultLocation))) {
                            // delete all assignments that are invalidated by this current instruction
                            // if it is an assignment
                            copies.remove(assignableName);
                        }
                    }
                    // insert this new pair into copies
                    var operand = hasResult.getComputationNoArray();
                    operand.ifPresent(value -> copies.put(resultLocation, value));
                }
            }
            if (!added)
                newTacList.addCode(threeAddressCode);
        }
        basicBlock.threeAddressCodeList = newTacList;
        return newTacList
                .getCodes()
                .equals(oldTacList);
    }

    public static void performGlobalCopyPropagation(BasicBlock entryBasicBlock, Set<AbstractName> globalVariables) {
        var availableCopies = new AvailableCopies(entryBasicBlock);
        var availableCopiesIn = availableCopies.availableCopies;
        var basicBlocks = DataFlowAnalysis.getReversePostOrder(entryBasicBlock);

        // remove all copy instructions which involve global variables
        availableCopiesIn.forEach(((basicBlock, assigns) -> assigns
                .keySet()
                .forEach(resultLocation -> {
                    if (globalVariables.contains(resultLocation)) {
                        assigns.remove(resultLocation);
                    }
                })));

        // perform copy propagation until no more changes are observed
        var notConverged = true;
        while (notConverged) {
            notConverged = false;
            for (BasicBlock basicBlock : basicBlocks) {
                if (!performLocalCopyPropagation(basicBlock, availableCopiesIn.get(basicBlock))) {
                    notConverged = true;
                }
            }
        }
    }
}
