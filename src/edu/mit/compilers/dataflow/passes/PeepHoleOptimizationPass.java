package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.codegen.codes.ConditionalJump;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.codegen.names.AbstractName;

public class PeepHoleOptimizationPass extends OptimizationPass {
    public PeepHoleOptimizationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    private void eliminateRedundantJumps() {
        for (var basicBlock: basicBlocks) {
            var indicesToRemove = new ArrayList<Integer>();
            for (var indexOfCode = 0; indexOfCode < basicBlock.threeAddressCodeList.size(); indexOfCode++) {
                var tac = basicBlock.threeAddressCodeList.get(indexOfCode);
                if (tac instanceof UnconditionalJump) {
                    var unconditionalJump = (UnconditionalJump) tac;

                    if (indexOfCode + 1 < basicBlock.threeAddressCodeList.size()) {
                        var nextTac = basicBlock.threeAddressCodeList.get(indexOfCode + 1);
                        if (nextTac instanceof Label) {
                            var label = (Label) nextTac;
                            if (unconditionalJump.goToLabel.equals(label)) {
                                indicesToRemove.add(indexOfCode);
                            }
                        }
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
    }

    private Set<Label> findAllLabelsJumpedTo() {
        var allLabelsJumpedTo = new HashSet<Label>();
        for (var basicBlock : basicBlocks) {
            for (ThreeAddressCode tac : basicBlock.threeAddressCodeList.flatten()) {
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
            for (var indexOfCode = 0; indexOfCode < basicBlock.threeAddressCodeList.size(); indexOfCode++) {
                var tac = basicBlock.threeAddressCodeList.get(indexOfCode);
                if (tac instanceof Label) {
                    var label = (Label) tac;
                    if (!allLabelsJumpedTo.contains(label)) {
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
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.codes();
        eliminateRedundantJumps();
        eliminateUnUsedJumps();
        return oldCodes.equals(entryBlock.threeAddressCodeList.getCodes());
    }
}
