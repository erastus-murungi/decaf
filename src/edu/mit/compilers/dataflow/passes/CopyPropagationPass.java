package edu.mit.compilers.dataflow.passes;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.dataflow.analyses.AvailableCopies;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;

import java.util.*;

public class CopyPropagationPass extends OptimizationPass {
    public CopyPropagationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }


    private static void propagateCopy(HasOperand hasOperand, HashMap<AbstractName, Operand> copies) {
        boolean notConverged = true;
        while (notConverged) {
            // we have to do this in a while loop because of how copy replacements propagate
            // for instance, lets imagine we have a = k
            // it possible that our copies map has y `replaces` k, and x `replaces` y and $0 `replaces` x
            // we want to eventually propagate so that a = $0
            notConverged = false;
            for (var toBeReplaced : hasOperand.getOperandNamesNoArray()) {
                if (copies.get(toBeReplaced) instanceof UnmodifiedOperand) {
                    var replacer = ((UnmodifiedOperand) copies.get(toBeReplaced)).abstractName;
                    if (!isTrivialAssignment((Instruction) hasOperand)) {
                        hasOperand.replace(toBeReplaced, replacer);
                    } else {
                        continue;
                    }
                    // it is not enough to set `converged` to true if we have found our replacer
                    // we do this extra check, i.e the check : "!replacerName.equals(abstractName)"
                    // because our copies map sometimes contains entries like "x `replaces` x"
                    // without this check, we sometimes enter an infinite loop
                    if (!replacer.equals(toBeReplaced)) {
                        notConverged = true;
                    }
                }
            }
        }
    }

    private boolean performLocalCopyPropagation(BasicBlock basicBlock, HashMap<AbstractName, Operand> copies) {
        // we need a reference tac list to know whether any changes occurred
        final var tacList = basicBlock.getCopyOfInstructionList();
        final var newTacList = basicBlock.instructionList;

        newTacList.clear();

        for (Instruction instruction : tacList) {
            // we only perform copy propagation on instructions with variables
            if (instruction instanceof HasOperand) {
                propagateCopy((HasOperand) instruction, copies);
            }

            if (instruction instanceof Store) {
                var hasResult = (Store) instruction;
                var resultLocation = hasResult.getStore();
                if (!(resultLocation instanceof ArrayName)) {
                    for (var assignableName : new ArrayList<>(copies.keySet())) {
                        var replacer = copies.get(assignableName);
                        if (assignableName.equals(resultLocation) ||
                                ((replacer instanceof UnmodifiedOperand) && ((UnmodifiedOperand) replacer).abstractName.equals(resultLocation))) {
                            // delete all assignments that are invalidated by this current instruction
                            // if it is an assignment
                            copies.remove(assignableName);
                        }
                    }
                    // insert this new pair into copies
                        var operand = hasResult.getOperandNoArrayNoGlobals(globalVariables);
                        operand.ifPresent(value -> copies.put(resultLocation, value));
                }
            }
            newTacList.add(instruction);
        }
        return newTacList.equals(tacList);
    }

    public void performGlobalCopyPropagation() {
        var availableCopiesIn = new AvailableCopies(entryBlock).availableCopies;

        // remove all copy instructions which involve global variables
        availableCopiesIn.forEach(((basicBlock, assigns) -> new ArrayList<>(assigns
                .keySet())
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

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        performGlobalCopyPropagation();
        return oldCodes.equals(entryBlock.instructionList);
    }
}
