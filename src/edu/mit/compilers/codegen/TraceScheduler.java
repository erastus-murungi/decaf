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
import edu.mit.compilers.codegen.codes.UnconditionalBranch;
import edu.mit.compilers.utils.TarjanSCC;

/**
 * Finds a greedy trace of the basic blocks in a CFG
 */
public class TraceScheduler {
    private final Method method;
    private List<BasicBlock> basicBlocks;
    private List<InstructionList> trace;

    public TraceScheduler(Method method) {
        this.method = method;
        findBasicBlocks(method);
        computeTrace();
    }

    public static List<InstructionList> getInstructionTrace(Method method) {
        return new TraceScheduler(method).trace;
    }

    public static InstructionList flattenIr(Method method) {
        return new InstructionList(new TraceScheduler(method).trace
                .stream()
                .flatMap(Collection::stream)
                .toList());
    }

    private void findBasicBlocks(Method method) {
        basicBlocks = TarjanSCC.getReversePostOrder(method.entryBlock);
    }

    private Optional<BasicBlock> getNotTracedBlock(Set<BasicBlock> seenBlocks) {
        return basicBlocks.stream()
                .filter(basicBlock -> !seenBlocks.contains(basicBlock))
                .findFirst();
    }

    private void traceBasicBlock(BasicBlock basicBlock, Set<BasicBlock> tracedBasicBlocksSet) {
        if (tracedBasicBlocksSet.contains(basicBlock)) {
            trace.add(InstructionList.of(new UnconditionalBranch(basicBlock)));
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
