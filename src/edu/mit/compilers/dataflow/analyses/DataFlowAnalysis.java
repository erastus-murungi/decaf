package edu.mit.compilers.dataflow.analyses;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.dataflow.Direction;
import edu.mit.compilers.utils.TarjanSCC;

public abstract class DataFlowAnalysis<T> {
    public Map<BasicBlock, Set<T>> out;
    public Map<BasicBlock, Set<T>> in;
    Set<T> allTS;
    // the list of all basic blocks
    List<BasicBlock> basicBlocks;
    NOP entryBlock;
    NOP exitBlock;

    public DataFlowAnalysis(BasicBlock basicBlock) {
        attachEntryNode(basicBlock);
        findAllBasicBlocksInReversePostOrder();
        findExitBlock();
        computeUniversalSetsOfValues();
        initializeWorkSets();
        runWorkList();
    }

    public static Map<Instruction, Integer> getInstructionToIndexMapping(InstructionList instructionList) {
        var instructionToIndexMapping = new LinkedHashMap<Instruction, Integer>();
        var index = 0;
        for (Instruction instruction : instructionList) {
            instructionToIndexMapping.put(instruction, index);
            ++index;
        }
        return instructionToIndexMapping;
    }

    Set<T> in(BasicBlock basicBlock) {
        return in.get(basicBlock);
    }

    Set<T> out(BasicBlock basicBlock) {
        return out.get(basicBlock);
    }

    private void attachEntryNode(BasicBlock basicBlock) {
        entryBlock = new NOP("Entry", NOP.NOPType.METHOD_ENTRY);
        entryBlock.setSuccessor(basicBlock);
        basicBlock.addPredecessor(entryBlock);
    }

    public abstract void computeUniversalSetsOfValues();

    private void findAllBasicBlocksInReversePostOrder() {
        basicBlocks = TarjanSCC.getReversePostOrder(entryBlock);
    }

    private void findExitBlock() {
        List<NOP> exitBlockList = basicBlocks
                .stream()
                .filter(basicBlock -> basicBlock instanceof NOP)
                .map(basicBlock -> (NOP) basicBlock)
                .filter(NOP::isExitNop)
                .toList();
        checkState(exitBlockList.size() == 1, "expected 1 exit node, found " + exitBlockList.size());
        exitBlock = exitBlockList.get(0);
    }

    public abstract Set<T> meet(BasicBlock basicBlock);

    public abstract Set<T> transferFunction(T domainElement);

    public abstract Direction direction();

    public abstract void initializeWorkSets();

    public abstract void runWorkList();

    public String getResultForPrint() {
        return Stream
                .of(basicBlocks)
                .flatMap(Collection::stream)
                .map(basicBlock -> in.get(basicBlock))
                .flatMap(Collection::stream)
                .map(T::toString)
                .collect(Collectors.joining("\n"));
    }
}
