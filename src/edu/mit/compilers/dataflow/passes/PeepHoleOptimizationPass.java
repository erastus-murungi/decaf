package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.ConditionalJump;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;

public class PeepHoleOptimizationPass extends OptimizationPass {
    public PeepHoleOptimizationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    private void eliminateRedundantJumps() {
        for (var basicBlock: basicBlocks) {
            var indicesToRemove = new ArrayList<Integer>();
            for (var indexOfCode = 0; indexOfCode < basicBlock.instructionList.size(); indexOfCode++) {
                var tac = basicBlock.instructionList.get(indexOfCode);
                if (tac instanceof UnconditionalJump) {
                    var unconditionalJump = (UnconditionalJump) tac;

                    if (indexOfCode + 1 < basicBlock.instructionList.size()) {
                        var nextTac = basicBlock.instructionList.get(indexOfCode + 1);
                        if (nextTac instanceof Label) {
                            var label = (Label) nextTac;
                            if (unconditionalJump.goToLabel.equals(label)) {
                                indicesToRemove.add(indexOfCode);
                            }
                        }
                    }
                }
            }
            if (basicBlock.instructionList.lastCode().isPresent()) {
                var last = basicBlock.instructionList.lastCode().get();
                var nextBlock = basicBlock.instructionList.nextInstructionList;
                while (nextBlock != null && nextBlock.isEmpty())
                    nextBlock = nextBlock.nextInstructionList;
                if (nextBlock != null && nextBlock.isEmpty()) {
                    var first = nextBlock.firstCode();
                    if (last instanceof UnconditionalJump) {
                        if (first instanceof Label) {
                            if (((UnconditionalJump) last).goToLabel.label.equals(((Label) first).label)) {
                                basicBlock.instructionList.remove(basicBlock.instructionList.size() - 1);
                                nextBlock.remove(0);
                            }
                        }
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
        }
    }

    private Set<Label> findAllLabelsJumpedTo() {
        var allLabelsJumpedTo = new HashSet<Label>();
        for (var basicBlock : basicBlocks) {
            for (Instruction tac : basicBlock.instructionList.flatten()) {
                if (tac instanceof ConditionalJump) {
                    allLabelsJumpedTo.add(((ConditionalJump) tac).trueLabel);
                } else if (tac instanceof UnconditionalJump) {
                    allLabelsJumpedTo.add(((UnconditionalJump) tac).goToLabel);
                }
            }
        }
        return allLabelsJumpedTo;
    }

    private void eliminateUnUsedJumps() {
        final var allLabelsJumpedTo = findAllLabelsJumpedTo();
        for (var basicBlock : basicBlocks) {
            var indicesToRemove = new ArrayList<Integer>();
            for (var indexOfCode = 0; indexOfCode < basicBlock.instructionList.size(); indexOfCode++) {
                var tac = basicBlock.instructionList.get(indexOfCode);
                if (tac instanceof Label) {
                    var label = (Label) tac;
                    if (!allLabelsJumpedTo.contains(label)) {
                        indicesToRemove.add(indexOfCode);
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
        }
    }

    private void removeTrivialBoundsChecks() {
        for (var basicBlock : basicBlocks) {
            var indicesToRemove = new ArrayList<Integer>();
            for (var indexOfInstruction = 0; indexOfInstruction < basicBlock.instructionList.size(); indexOfInstruction++) {
                var instruction = basicBlock.instructionList.get(indexOfInstruction);
                if (instruction instanceof ArrayBoundsCheck) {
                    ArrayBoundsCheck arrayBoundsCheck = (ArrayBoundsCheck) instruction;
                    if (arrayBoundsCheck.getAddress.getIndex() instanceof ConstantName) {
                        var index = Long.parseLong(arrayBoundsCheck.getAddress.getIndex().value);
                        var length = Long.parseLong(arrayBoundsCheck.getAddress.getLength().orElseThrow().value);
                        if (index >= 0 && index < length) {
                            indicesToRemove.add(indexOfInstruction);
                        }
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
        }
    }



    @Override
    public boolean run() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        eliminateRedundantJumps();
        eliminateUnUsedJumps();
        removeTrivialBoundsChecks();
        return oldCodes.equals(entryBlock.instructionList);
    }
}
