package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;

/**
 * Finds a greedy trace of the basic blocks in a CFG
 */
public class TraceScheduler {
    public static final int MAX_JUMP_REDUCTION_TRIES = 10;
    private List<BasicBlock> basicBlocks;
    private List<InstructionList> trace;
    private final boolean addJumps;
    private final Method method;

    public List<InstructionList> getInstructionTrace() {
        return reduceNumberOfJumps(trace);
    }

    private static int findBasicBlockWithLabel(List<InstructionList> basicBlocks, Label targetLabel) {
        for (int indexOfBasicBlock = 0; indexOfBasicBlock < basicBlocks.size(); indexOfBasicBlock++) {
            var instructionList = basicBlocks.get(indexOfBasicBlock);
            if (!instructionList.isEmpty() && instructionList.get(0) instanceof Label) {
                Label label = (Label) (instructionList.get(0));
                if (label.equals(targetLabel)) {
                    return indexOfBasicBlock;
                }
            }
        }
        return -1;
    }

    private static Label getLabelFromInstructionsWithJumps(Instruction instruction) {
        if (instruction instanceof UnconditionalJump) {
            return ((UnconditionalJump) instruction).goToLabel;
        } else {
            return ((ConditionalBranch) instruction).falseLabel;
        }
    }

    private static List<InstructionList> reduceNumberOfJumps(List<InstructionList> basicBlocks) {
//        boolean changesHappened;
//        for (int i = 0; i < MAX_JUMP_REDUCTION_TRIES; i++) {
//            changesHappened = false;
//            for (int indexOfBasicBlock = 0; indexOfBasicBlock < basicBlocks.size(); indexOfBasicBlock++) {
//                var instructionList = basicBlocks.get(indexOfBasicBlock);
//                if (instructionList.lastInstruction()
//                                   .isPresent()) {
//                    Instruction instruction = instructionList.lastInstruction()
//                                                             .get();
//                    if (instruction instanceof UnconditionalJump || instruction instanceof ConditionalJump) {
//                        var goToLabel = getLabelFromInstructionsWithJumps(instruction);
//                        if (!goToLabel.isExitLabel()) {
//                            int indexOfJumpedTo = findBasicBlockWithLabel(basicBlocks, goToLabel);
//                            var subList = new ArrayList<>(basicBlocks.subList(indexOfJumpedTo, basicBlocks.size()));
//                            basicBlocks.subList(indexOfJumpedTo, basicBlocks.size())
//                                       .clear();
//                            if (instruction instanceof ConditionalJump)
//                                basicBlocks.addAll(indexOfBasicBlock + 2, subList);
//                            else
//                                basicBlocks.addAll(Math.min(indexOfBasicBlock, indexOfJumpedTo), subList);
//                            changesHappened = true;
//                        }
//                    }
//                }
//            }
//            for (int indexOfBasicBlock = 0; indexOfBasicBlock < basicBlocks.size(); indexOfBasicBlock++) {
//                var instructionList = basicBlocks.get(indexOfBasicBlock);
//                if (!instructionList.isEmpty() && instructionList.get(0) instanceof Label) {
//                    Label goToLabel = (Label) (instructionList.get(0));
//                    if (goToLabel.isExitLabel()) {
//                        basicBlocks.add(basicBlocks.remove(indexOfBasicBlock));
//                    }
//                }
//            }
//            if (!changesHappened)
//                break;
//        }

        return basicBlocks;
    }

    public static InstructionList flattenIr(Method method, boolean addJumps) {
        return new InstructionList(new TraceScheduler(method, addJumps).getInstructionTrace()
                                                                       .stream()
                                                                       .flatMap(Collection::stream)
                                                                       .collect(Collectors.toUnmodifiableList()));
    }

    public static InstructionList flattenIr(Method method) {
        return flattenIr(method, true);
    }

    private void findBasicBlocks(Method method) {
        basicBlocks = DataFlowAnalysis.getReversePostOrder(method.entryBlock);
    }

    public TraceScheduler(Method method, boolean addJumps) {
        this.addJumps = addJumps;
        this.method = method;
        findBasicBlocks(method);
        computeTrace();
    }

    public TraceScheduler(Method method) {
        this(method, true /*addJumps*/);
    }

    private Optional<BasicBlock> getNotTracedBlock(Set<BasicBlock> seenBlocks) {
        return basicBlocks.stream()
                          .filter(basicBlock -> !seenBlocks.contains(basicBlock))
                          .findFirst();
    }

    private void traceBasicBlock(BasicBlock basicBlock,
                                 Set<BasicBlock> tracedBasicBlocksSet) {
        if (tracedBasicBlocksSet.contains(basicBlock)) {
            if (addJumps)
                trace.add(InstructionList.of(new UnconditionalJump(basicBlock.getLabel())));
            return;
        }
        trace.add(basicBlock.getInstructionList());
        tracedBasicBlocksSet.add(basicBlock);
        // note: basicBlock.getSuccessors().get(0) is either an autoChild or the trueChild
        if (!(basicBlock instanceof NOP))
            traceBasicBlock(basicBlock.getSuccessors()
                                      .get(0), tracedBasicBlocksSet);
    }

    public void computeTrace() {
        trace = new ArrayList<>();
        Set<BasicBlock> tracedBasicBlocksSet = new HashSet<>();
        tracedBasicBlocksSet.add(method.exitBlock);

        var notTracedBasicBlock = getNotTracedBlock(tracedBasicBlocksSet);
        while (notTracedBasicBlock.isPresent()) {
            var basicBlock = notTracedBasicBlock.get();
            if (tracedBasicBlocksSet.contains(basicBlock))
                continue;
            traceBasicBlock(basicBlock, tracedBasicBlocksSet);
            notTracedBasicBlock = getNotTracedBlock(tracedBasicBlocksSet);
        }
        trace.add(method.exitBlock.getInstructionList());
    }
}
