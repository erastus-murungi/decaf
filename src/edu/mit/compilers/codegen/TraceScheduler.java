package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;

/**
 * Finds a greedy trace of the basic blocks in a CFG
 */
public class TraceScheduler {
    private List<BasicBlock> basicBlocks;
    private List<InstructionList> trace;
    private final boolean addJumps;
    private final Method method;

    public List<InstructionList> getInstructionTrace() {
        return trace;
    }

    public static InstructionList flattenIr(Method method, boolean addJumps) {
        return new InstructionList(new TraceScheduler(method, addJumps).getInstructionTrace()
                                                                       .stream()
                                                                       .flatMap(Collection::stream)
                                                                       .toList());
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

    private void traceBasicBlock(BasicBlock basicBlock, Set<BasicBlock> tracedBasicBlocksSet) {
        if (tracedBasicBlocksSet.contains(basicBlock)) {
            if (addJumps) trace.add(InstructionList.of(new UnconditionalJump(basicBlock.getLabel())));
            return;
        }
        trace.add(basicBlock.getInstructionList());
        tracedBasicBlocksSet.add(basicBlock);
        // note: basicBlock.getSuccessors().get(0) is either an autoChild or the trueChild
        if (!(basicBlock instanceof NOP)) traceBasicBlock(basicBlock.getSuccessors()
                                                                    .get(0), tracedBasicBlocksSet);
    }

    public void computeTrace() {
        trace = new ArrayList<>();
        Set<BasicBlock> tracedBasicBlocksSet = new HashSet<>();
        tracedBasicBlocksSet.add(method.exitBlock);

        var notTracedBasicBlock = getNotTracedBlock(tracedBasicBlocksSet);
        while (notTracedBasicBlock.isPresent()) {
            var basicBlock = notTracedBasicBlock.get();
            if (tracedBasicBlocksSet.contains(basicBlock)) continue;
            traceBasicBlock(basicBlock, tracedBasicBlocksSet);
            notTracedBasicBlock = getNotTracedBlock(tracedBasicBlocksSet);
        }
        trace.add(method.exitBlock.getInstructionList());
    }
}
