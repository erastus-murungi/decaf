package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.StringConstantName;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;

/**
 * A local variable that is assigned a value but is not read by any subsequent instruction is referred to as a dead store
 * This pass removes all dead stores from a program
 */

public class DeadStoreEliminationPass extends OptimizationPass {
    public DeadStoreEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    /**
     * Returns true is a name is a constant (value cannot be changed)
     * For instance a, %1, *b[1] return false while `const 1` and @.string_0 return true
     *
     * @param abstractName the name to check
     * @return true is abstractName is a constant
     */
    private boolean isConstant(AbstractName abstractName) {
        return abstractName instanceof ConstantName || abstractName instanceof StringConstantName;
    }

    /**
     * Return true if all the variables on the right-hand side of an instruction are constants
     *
     * @param instruction the ThreeAddressCode to be tested
     * @return true if a threeAddressCode has a right-hand side and all the operands on the right-hand side are constants
     */
    private boolean allOperandNamesConstant(Instruction instruction) {
        if (instruction instanceof HasOperand)
            return ((HasOperand) instruction).getOperandNames()
                    .stream()
                    .allMatch(this::isConstant);
        return false;
    }

    /**
     * @param basicBlock        basic block whose dead stores we are attempting to eliminate
     * @param basicBlockLiveOut set of names which are live going out of this block
     */
    private void backwardRun(BasicBlock basicBlock, Set<AbstractName> basicBlockLiveOut) {
        // we will be iterating backward
        var copyOfInstructionList = basicBlock.getCopyOfInstructionList();
        Collections.reverse(copyOfInstructionList);

        // this is the list we will be updating, we don't use a new arraylist because we want to maintain all the other
        // internal members of the basicBlock, such as `nextInstruction`
        final var instructionListToUpdate = basicBlock.instructionList;
        instructionListToUpdate.clear();

        final var namesUsedSoFar = new HashSet<AbstractName>();
        // we iterate in reverse
        for (var possibleStoreInstruction : copyOfInstructionList) {
            // always ignore assignments like a = a, a[i] = a[i]
            if (possibleStoreInstruction.repr().contains("kernel"))
                System.out.println("stop");
            if (isTrivialAssignment(possibleStoreInstruction))
                continue;

            if (possibleStoreInstruction instanceof HasOperand) {
                if (((HasOperand) possibleStoreInstruction).getOperandNames().stream().anyMatch(abstractName -> abstractName instanceof ArrayName)) {
                    instructionListToUpdate.add(possibleStoreInstruction);
                    namesUsedSoFar.addAll(((HasOperand) possibleStoreInstruction).getOperandNames());
                    continue;
                }
            }

            // check if this a possible store instruction
            if (possibleStoreInstruction instanceof Store) {
                // grab the store location
                var store = ((Store) possibleStoreInstruction).getStore();

                // ignore arrays for now
                if (!(store instanceof ArrayName)) {
                    // do not eliminate global variables and store instructions where the
                    // right-hand side is composed of all constants, for example a = 1 + 2 or a = 3
                    // if (we are in the main method then we can afford to ignore global variables)
                    if ((globalVariables.contains(store) && !methodBegin.isMain()) || allOperandNamesConstant(possibleStoreInstruction)) {
                        if (possibleStoreInstruction instanceof HasOperand)
                            namesUsedSoFar.addAll(((HasOperand) possibleStoreInstruction).getOperandNames());
                        instructionListToUpdate.add(possibleStoreInstruction);
                        continue;
                    }
                    // if we don't use this variable after this block
                    if (!basicBlockLiveOut.contains(store) &&
                            // and we don't use this variable in this block, after this program point
                            !namesUsedSoFar.contains(store)) {
                        continue;
                    }
                }
            }
            if (possibleStoreInstruction instanceof HasOperand)
                namesUsedSoFar.addAll(((HasOperand) possibleStoreInstruction).getOperandNames());

            instructionListToUpdate.add(possibleStoreInstruction);
        }
    }

    private void forwardRun(BasicBlock basicBlock, Set<AbstractName> basicBlockLiveOut) {
        var copyOfInstructionList = new ArrayList<>(basicBlock.instructionList);
        Collections.reverse(copyOfInstructionList);
        final var instructionListToUpdate = basicBlock.instructionList;
        instructionListToUpdate.clear();

        int indexOfInstruction = -1;
        for (var possibleStoreInstruction : copyOfInstructionList) {
            if (possibleStoreInstruction.repr().contains("4433"))
                System.out.println("stop");
            ++indexOfInstruction;
            if (possibleStoreInstruction instanceof Store) {
                if (possibleStoreInstruction instanceof FunctionCallWithResult) {
                    instructionListToUpdate.add(possibleStoreInstruction);
                    continue;
                }
                if (possibleStoreInstruction instanceof HasOperand) {
                    if (((HasOperand) possibleStoreInstruction).getOperandNames().stream().anyMatch(abstractName -> abstractName instanceof ArrayName)) {
                        instructionListToUpdate.add(possibleStoreInstruction);
                        continue;
                    }
                }

                var storeInstruction = ((Store) possibleStoreInstruction);
                var store = storeInstruction.getStore();

                if (!(store instanceof ArrayName)) {
                    if (globalVariables.contains(store) && !methodBegin.isMain()) {
                        instructionListToUpdate.add(possibleStoreInstruction);
                        continue;
                    }
                    if (!storeIsUsedBeforeNextReassignmentInBlock(storeInstruction, copyOfInstructionList, indexOfInstruction)
                            && !(isLastStoreInstructionInBlock(storeInstruction, copyOfInstructionList, indexOfInstruction)
                            && basicBlockLiveOut.contains(store)))
                        continue;
                }
            }
            instructionListToUpdate.add(possibleStoreInstruction);
        }
    }

    private void performDeadStoreElimination() {
        final var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);

        for (var basicBlock : basicBlocks) {
            final var basicBlockLiveOut = Collections.unmodifiableSet(liveVariableAnalysis.liveOut(basicBlock));

            backwardRun(basicBlock, basicBlockLiveOut);
            forwardRun(basicBlock, basicBlockLiveOut);
        }
    }

    /**
     * Checks whether a store is used on some right-hand-side before its next reassignment in the same block
     *
     * @param storeInstruction               a store instruction to be tested
     * @param blockInstructionList           the instruction list containing {@code storeInstruction}
     * @param indexOfStoreInstructionInBlock the index of {@code storeInstruction} in {@code blockInstructionList}
     * @return true if {@code storeInstruction} is used on some right-hand-side before its next reassignment in the same block
     * @implNote if there is no other reassignment of the store, this method returns true
     */
    private boolean storeIsUsedBeforeNextReassignmentInBlock(Store storeInstruction,
                                                             List<Instruction> blockInstructionList,
                                                             int indexOfStoreInstructionInBlock) {
        // this set stores all the names used in the right hands sides of instructions starting from
        // blockInstructionList[`indexOfStoreInstructionInBlock`] henceforth
        var usedNames = new HashSet<AbstractName>();

        // Because blockInstructionList[indexOfStoreInstructionInBlock] == storeInstruction = `some operand`
        // we start our check from indexOfStoreInstructionInBlock + 1
        for (var indexOfInstruction = indexOfStoreInstructionInBlock + 1; indexOfInstruction < blockInstructionList.size(); indexOfInstruction++) {
            var possibleStoreInstruction = blockInstructionList.get(indexOfInstruction);
            if (possibleStoreInstruction instanceof Store) {
                var candidateStoreInstruction = (Store) possibleStoreInstruction;
                if (candidateStoreInstruction.getStore()
                        .equals(storeInstruction.getStore())) {
                    /* add the operands
                     * Before breaking, we add the operands of this assignment too
                     * we add this extra check because of stores of inputs like:
                     *
                     * 0: foo -> void {
                     * 1:        a = const 0
                     * 2:        b = const 1
                     * 3:        d = const 3
                     * 4:        d: int = b + d
                     * 5:        a: int = a + b
                     * 6:        call bar
                     * &: }
                     *
                     *  While checking whether assignment 1: (a = const 0) is redefined, we first stop at  line 5 (a: int = a + b)
                     *  We first add the operands (a, b) to `usedNames` before breaking
                     *  Without doing so we report that a = 0 is never used before the reassignment at line 5
                     */
                    if (candidateStoreInstruction instanceof HasOperand) {
                        var storeInstructionWithOperand = ((HasOperand) candidateStoreInstruction);
                        usedNames.addAll(storeInstructionWithOperand.getOperandNames());
                    }
                    break;
                }
            }
            // add all names on the right-hand side;
            if (possibleStoreInstruction instanceof HasOperand) {
                var storeInstructionWithOperand = ((HasOperand) possibleStoreInstruction);
                usedNames.addAll(storeInstructionWithOperand.getOperandNames());
            }
        }
        return usedNames.contains(storeInstruction.getStore());
    }

    /**
     * Checks if an assignment to a variable is the last one to assign to that variable in that basic block
     *
     * @param storeInstruction               the Store Instruction whose "last-ness" is to be tested
     * @param blockInstructionList           the {@link InstructionList} which contains an
     *                                       assignment to {@code store}
     * @param indexOfStoreInstructionInBlock the index of assignment to {@code store} in the tac-list
     * @return true is there is no other assigment which overwrites {@code store}
     *
     * <p>For example:
     * <pre> {@code
     *
     * foo: -> void {
     *         a: int = const 0
     *         b: int = const 0
     *         c: int = const 0
     *         a: int = const 5
     *         push c
     *         push @.string_0
     *         call int @printf
     *     }
     *
     * }</pre>
     *
     * <pre> {@code isLastStoreInBlock(a, fooTacList, 1) == false}</pre>
     * <pre> {@code isLastStoreInBlock(c, fooTacList, 3) == true}</pre>
     */
    private boolean isLastStoreInstructionInBlock(Store storeInstruction, List<Instruction> blockInstructionList, int indexOfStoreInstructionInBlock) {
        // Because blockInstructionList[indexOfStoreInstructionInBlock] == storeInstruction = `some operand`
        // we start our check from indexOfStoreInstructionInBlock + 1
        assert blockInstructionList.get(indexOfStoreInstructionInBlock)
                .equals(storeInstruction);

        for (var indexOfInstruction = indexOfStoreInstructionInBlock + 1; indexOfInstruction < blockInstructionList.size(); indexOfInstruction++) {
            var possibleStoreInstruction = blockInstructionList.get(indexOfInstruction);
            if (possibleStoreInstruction instanceof Store) {
                var candidateStoreInstruction = (Store) possibleStoreInstruction;
                // check if we are indeed overwriting the store
                if (candidateStoreInstruction.getStore()
                        .equals(storeInstruction.getStore())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        performDeadStoreElimination();
        return oldCodes.equals(entryBlock.instructionList);
    }
}
