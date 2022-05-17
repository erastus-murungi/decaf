package edu.mit.compilers.codegen;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;

/**
 * Finds a greedy trace of the basic blocks in a CFG
 */
public class TraceScheduler {
    private List<BasicBlock> basicBlocks;
    private LinkedList<InstructionList> trace;
    private final boolean addJumps;
    private MethodBegin methodBegin;

    public LinkedList<InstructionList> getInstructionTrace() {
        return trace;
    }

    public static InstructionList flattenIr(MethodBegin methodBegin, boolean addJumps) {
        return new InstructionList(new TraceScheduler(methodBegin, addJumps).getInstructionTrace()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList()));
    }

    public static InstructionList flattenIr(MethodBegin methodBegin) {
        return flattenIr(methodBegin, true);
    }

    private void findBasicBlocks(MethodBegin methodBegin) {
        basicBlocks = DataFlowAnalysis.getReversePostOrder(methodBegin.entryBlock);
    }

    public TraceScheduler(MethodBegin methodBegin, boolean addJumps) {
        this.addJumps = addJumps;
        this.methodBegin = methodBegin;
        findBasicBlocks(methodBegin);
        computeTrace();
    }

    public TraceScheduler(MethodBegin methodBegin) {
        this(methodBegin, true /*addJumps*/);
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
        trace.add(basicBlock.instructionList);
        tracedBasicBlocksSet.add(basicBlock);
        // note: basicBlock.getSuccessors().get(0) is either an autoChild or the trueChild
        if (!(basicBlock instanceof NOP))
            traceBasicBlock(basicBlock.getSuccessors()
                    .get(0), tracedBasicBlocksSet);
    }

    public void computeTrace() {
        trace = new LinkedList<>();
        Set<BasicBlock> tracedBasicBlocksSet = new HashSet<>();
        tracedBasicBlocksSet.add(methodBegin.exitBlock);

        var notTracedBasicBlock = getNotTracedBlock(tracedBasicBlocksSet);
        while (notTracedBasicBlock.isPresent()) {
            var basicBlock = notTracedBasicBlock.get();
            if (tracedBasicBlocksSet.contains(basicBlock))
                continue;
            traceBasicBlock(basicBlock, tracedBasicBlocksSet);
            notTracedBasicBlock = getNotTracedBlock(tracedBasicBlocksSet);
        }
        trace.add(methodBegin.exitBlock.instructionList);
    }
}
