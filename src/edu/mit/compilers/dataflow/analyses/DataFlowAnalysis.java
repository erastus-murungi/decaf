package edu.mit.compilers.dataflow.analyses;

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

public abstract class DataFlowAnalysis<Value> {
    public Map<BasicBlock, Set<Value>> out;
    public Map<BasicBlock, Set<Value>> in;
    Set<Value> allValues;
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

    Set<Value> in(BasicBlock basicBlock) {
        return in.get(basicBlock);
    }

    Set<Value> out(BasicBlock basicBlock) {
        return out.get(basicBlock);
    }

    private void attachEntryNode(BasicBlock basicBlock) {
        entryBlock = new NOP("Entry");
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
                .filter(nop -> nop != entryBlock)
                .toList();
        if (exitBlockList.size() != 1)
            throw new IllegalStateException("expected 1 exit node, found " + exitBlockList.size());
        exitBlock = exitBlockList.get(0);
    }

    public abstract Set<Value> meet(BasicBlock basicBlock);

    public abstract Set<Value> transferFunction(Value domainElement);

    public abstract Direction direction();

    public abstract void initializeWorkSets();

    public abstract void runWorkList();

    public String getResultForPrint() {
        return Stream
                .of(basicBlocks)
                .flatMap(Collection::stream)
                .map(basicBlock -> in.get(basicBlock))
                .flatMap(Collection::stream)
                .map(Value::toString)
                .collect(Collectors.joining("\n"));
    }
}
