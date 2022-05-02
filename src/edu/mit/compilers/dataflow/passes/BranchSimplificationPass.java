package edu.mit.compilers.dataflow.passes;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.BasicBlockBranchLess;
import edu.mit.compilers.cfg.BasicBlockWithBranch;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.codes.ConditionalJump;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.AbstractName;

public class BranchSimplificationPass extends OptimizationPass {
    HashMap<ThreeAddressCodeList, Set<ThreeAddressCodeList>> predecessors;

    public BranchSimplificationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
        buildDependencyGraph();
    }

    private boolean isTrue(ConditionalJump conditionalJump) {
        return conditionalJump.condition.equals(InstructionSimplifyPass.mOne);
    }

    private boolean isFalse(ConditionalJump conditionalJump) {
        return conditionalJump.condition.equals(InstructionSimplifyPass.mZero);
    }

    private void buildDependencyGraph() {
        predecessors = new HashMap<>();
        for (BasicBlock basicBlock : basicBlocks) {
            var tacList = basicBlock.threeAddressCodeList;
            var nextTacList = tacList.next;
            while (nextTacList != null) {
                predecessors.computeIfAbsent(nextTacList, (key) -> new HashSet<>());
                predecessors.get(nextTacList).add(tacList);
                tacList = nextTacList;
                nextTacList = tacList.next;
            }
        }
    }

    private Optional<ConditionalJump> getConditional(BasicBlockWithBranch basicBlockWithBranch) {
        return basicBlockWithBranch.threeAddressCodeList.getCodes()
                .stream()
                .dropWhile(threeAddressCode -> !(threeAddressCode instanceof ConditionalJump))
                .map(threeAddressCode -> (ConditionalJump) threeAddressCode)
                .findFirst();
//                .orElseThrow(() -> new IllegalArgumentException("No conditional jump found"));
    }

    private Boolean getStateOfBlockCondition(BasicBlockWithBranch basicBlockWithBranch) {
        Optional<ConditionalJump> conditionalJump = getConditional(basicBlockWithBranch);
        if (conditionalJump.isPresent()) {
            if (isTrue(conditionalJump.get())) {
                return true;
            } else if (isFalse(conditionalJump.get()))
                return false;
        }
        return null;
    }

    private void removeFalseBranch(BasicBlockWithBranch basicBlockWithBranch) {
        // if (true) || if (1)
        // just reduce this to the true branch

        var trueChild = basicBlockWithBranch.trueChild;
        var falseChild = basicBlockWithBranch.falseChild;

        for (BasicBlock predecessor : basicBlockWithBranch.getPredecessors()) {
            if (predecessor instanceof BasicBlockWithBranch) {
                var branchPredecessor = (BasicBlockWithBranch) predecessor;
                if (branchPredecessor.falseChild == basicBlockWithBranch) {
                    branchPredecessor.falseChild = trueChild;
                } else if (branchPredecessor.trueChild == basicBlockWithBranch) {
                    branchPredecessor.trueChild = trueChild;
                }
            } else {
                var branchLessPredecessor = (BasicBlockBranchLess) predecessor;
                if (trueChild instanceof BasicBlockWithBranch) {
                    branchLessPredecessor.autoChild = trueChild;
                    branchLessPredecessor.threeAddressCodeList.resetNext(trueChild.threeAddressCodeList);
                } else {
                    basicBlockWithBranch.threeAddressCodeList.reset(Collections.emptyList());
                    branchLessPredecessor.threeAddressCodeList.add(trueChild.threeAddressCodeList);
                    trueChild.threeAddressCodeList.reset(Collections.emptyList());
                    var collect = falseChild.threeAddressCodeList.getCodes()
                            .stream()
                            .dropWhile(BranchSimplificationPass::isNotExitLabel)
                            .collect(Collectors.toList());
                    branchLessPredecessor.threeAddressCodeList.getCodes()
                            .addAll(collect);
                    falseChild.threeAddressCodeList.reset(Collections.emptyList());
                }
            }
        }
    }

    private static boolean isNotExitLabel(ThreeAddressCode threeAddressCode) {
        if (threeAddressCode instanceof Label) {
            var label = (Label) threeAddressCode;
            return !label.label.startsWith("exit");
        }
        return true;
    }

    private void removeUselessBranches() {
        List<BasicBlockWithBranch> evaluateToTrue = new ArrayList<>();
        List<BasicBlockWithBranch> evaluateToFalse = new ArrayList<>();
        for (var basicBlock : basicBlocks) {
            if (basicBlock instanceof BasicBlockWithBranch) {
                BasicBlockWithBranch basicBlockWithBranch = (BasicBlockWithBranch) basicBlock;
                Boolean evaluatesTrueBranch = getStateOfBlockCondition(basicBlockWithBranch);
                if (evaluatesTrueBranch == null)
                    continue;
                if (evaluatesTrueBranch)
                    evaluateToTrue.add(basicBlockWithBranch);
                else
                    evaluateToFalse.add(basicBlockWithBranch);
            }
        }
        evaluateToTrue.forEach(this::removeFalseBranch);
    }


    @Override
    public boolean run() {
        final var oldCodes = entryBlock.codes();
        removeUselessBranches();
        return oldCodes.equals(entryBlock.threeAddressCodeList.getCodes());
    }
}
