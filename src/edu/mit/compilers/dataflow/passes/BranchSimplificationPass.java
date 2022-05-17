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
import edu.mit.compilers.cfg.BasicBlockWithBranch;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.ConditionalJump;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.codegen.names.AbstractName;

public class BranchSimplificationPass extends OptimizationPass {
    HashMap<Label, InstructionList> nextBlockInPath;
    boolean noChanges = true;

    public BranchSimplificationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
        buildBlockPath();
    }

    private boolean isTrue(ConditionalJump conditionalJump) {
        return conditionalJump.condition.equals(InstructionSimplifyPass.mOne);
    }

    private boolean isFalse(ConditionalJump conditionalJump) {
        return conditionalJump.condition.equals(InstructionSimplifyPass.mZero);
    }

    private void buildBlockPath() {
        nextBlockInPath = new HashMap<>();
        for (BasicBlock basicBlock : basicBlocks) {
            var tacList = basicBlock.instructionList;
            if (!tacList.isEmpty()) {
                Instruction tac = tacList.firstCode();
                if (tac instanceof Label)
                    nextBlockInPath.put(((Label) tac), tacList);
            }
            var nextTacList = tacList;
            while (nextTacList != null) {
                if (!nextTacList.isEmpty()) {
                    Instruction tac = nextTacList.firstCode();
                    if (tac instanceof Label)
                        nextBlockInPath.put(((Label) tac), nextTacList);
                }
                tacList = nextTacList;
                nextTacList = tacList;
            }
        }
    }

    private Optional<ConditionalJump> getConditional(InstructionList instructionList) {
        return instructionList
                .stream()
                .dropWhile(threeAddressCode -> !(threeAddressCode instanceof ConditionalJump))
                .map(threeAddressCode -> (ConditionalJump) threeAddressCode)
                .findFirst();
    }

    private Boolean getStateOfBlockCondition(BasicBlockWithBranch basicBlockWithBranch) {
        Optional<ConditionalJump> conditionalJump = getConditional(basicBlockWithBranch.instructionList);
        if (conditionalJump.isPresent()) {
            if (isTrue(conditionalJump.get())) {
                return true;
            } else if (isFalse(conditionalJump.get()))
                return false;
        }
        return null;
    }

    private void removeFalseBranch(BasicBlockWithBranch basicBlockWithBranch) {
//        // if (true) || if (1)
//        // just reduce this to the true branch
//        var trueChild = basicBlockWithBranch.trueChild;
//        var falseChild = basicBlockWithBranch.falseChild;
//        basicBlockWithBranch.instructionList.reset(Collections.emptyList());
//        Label label = (Label) falseChild.instructionList.get(0);
//        if (label.aliasLabels.isEmpty()) {
//            var collect = falseChild.instructionList
//                    .stream()
//                    .dropWhile(BranchSimplificationPass::isNotExitLabel)
//                    .collect(Collectors.toList());
//            trueChild.instructionList.addAll(collect);
//            falseChild.instructionList.reset(Collections.emptyList());
//        } else {
//            label.aliasLabels.remove(0);
//        }
//        noChanges = false;
    }

    private void removeTrueBranch(BasicBlockWithBranch basicBlockWithBranch) {
//        // if (false) || if (0)
//        // just reduce this to the true branch
//        var trueChild = basicBlockWithBranch.trueChild;
//        basicBlockWithBranch.instructionList.reset(Collections.emptyList());
//        trueChild.instructionList.reset(Collections.emptyList());
//
//        var visited = new HashSet<InstructionList>();
//        visited.add(trueChild.instructionList);
//        var nextTacList = trueChild.instructionList.nextInstructionList;
//        var labelToUnconditionalJump = new HashMap<Label, InstructionList>();
//        while (nextTacList != null) {
//            if (visited.contains(nextTacList))
//                break;
//            visited.add(nextTacList);
//            Optional<ConditionalJump> conditional = getConditional(nextTacList);
//            if (nextTacList.size() == 1 && nextTacList.get(0) instanceof UnconditionalJump) {
//                Label label = ((UnconditionalJump) nextTacList.get(0)).goToLabel;
//                labelToUnconditionalJump.put(label, nextTacList);
//                nextTacList = nextBlockInPath.get(label);
//                if (label == null)
//                    throw new IllegalArgumentException();
//                continue;
//            }
//            if (conditional.isPresent() || !nextTacList.isEmpty() && nextTacList.get(0) instanceof Label) {
//                Label label = (Label) nextTacList.get(0);
//                if (labelToUnconditionalJump.containsKey(label)) {
//                    labelToUnconditionalJump.get(label)
//                            .reset(Collections.emptyList());
//                    break;
//                }
//                labelToUnconditionalJump.put(label, nextTacList);
//                if (label.aliasLabels.isEmpty()) {
//                    nextTacList.reset(Collections.emptyList());
//                } else {
//                    label.aliasLabels.remove(0);
//                    break;
//                }
//            }
//            else {
//                nextTacList.reset(Collections.emptyList());
//            }
//            nextTacList = nextTacList.nextInstructionList;
//        }
//        noChanges = false;
    }

    private static boolean isNotExitLabel(Instruction instruction) {
//        if (instruction instanceof Label) {
//            var label = (Label) instruction;
//            return !label.label.startsWith("exit");
//        }
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
        if (evaluateToFalse.isEmpty() || evaluateToTrue.isEmpty())
            noChanges = true;
        evaluateToTrue.forEach(this::removeFalseBranch);
        evaluateToFalse.forEach(this::removeTrueBranch);
    }


    @Override
    public boolean run() {
        removeUselessBranches();
        return noChanges;
    }
}
