package edu.mit.compilers.dataflow.analyses;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.dataflow.Direction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DataFlowAnalysis<Value> {
    Set<Value> allValues;
    // the list of all basic blocks
    List<BasicBlock> basicBlocks;

    public Map<BasicBlock, Set<Value>> out;
    public Map<BasicBlock, Set<Value>> in;

    NOP entryBlock;
    NOP exitBlock;

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
        basicBlocks = DataFlowAnalysis.getReversePostOrder(entryBlock);
    }

    public DataFlowAnalysis(BasicBlock basicBlock) {
        attachEntryNode(basicBlock);
        findAllBasicBlocksInReversePostOrder();
        findExitBlock();
        computeUniversalSetsOfValues();
        initializeWorkSets();
        runWorkList();
    }

    private void findExitBlock() {
        List<NOP> exitBlockList = basicBlocks
                .stream()
                .filter(basicBlock -> basicBlock instanceof NOP)
                .map(basicBlock -> (NOP) basicBlock)
                .filter(nop -> nop != entryBlock)
                .collect(Collectors.toList());
        if (exitBlockList.size() != 1)
            throw new IllegalStateException("expected 1 exit node, found " + exitBlockList.size());
        exitBlock = exitBlockList.get(0);
    }

    public abstract Set<Value> meet(BasicBlock basicBlock);

    public abstract Set<Value> transferFunction(Value domainElement);

    public abstract Direction direction();

    public abstract void initializeWorkSets();

    public abstract void runWorkList();

    public HashSet<Value> difference(Set<Value> first, Set<Value> second) {
        var firstCopy = new HashSet<>(first);
        firstCopy.removeAll(second);
        return firstCopy;
    }

    public HashSet<Value> union(Set<Value> first, Set<Value> second) {
        var firstCopy = new HashSet<>(first);
        firstCopy.addAll(second);
        return firstCopy;
    }

    public static List<BasicBlock> getReversePostOrder(BasicBlock entryPoint) {
        List<List<BasicBlock>> stronglyConnectedComponents = findStronglyConnectedComponents(entryPoint);
        Collections.reverse(stronglyConnectedComponents);
        return stronglyConnectedComponents
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public static void correctPredecessors(BasicBlock block) {
        List<BasicBlock> basicBlocks = allNodes(block);

        for (BasicBlock basicBlock : basicBlocks)
            basicBlock.getPredecessors().clear();

        for (BasicBlock basicBlock : basicBlocks) {
            for (BasicBlock successor: basicBlock.getSuccessors()) {
                successor.addPredecessor(basicBlock);
            }
        }
    }

    public static List<BasicBlock> allNodes(BasicBlock entryPoint) {
        HashSet<BasicBlock> seen = new HashSet<>();
        Stack<BasicBlock> toExplore = new Stack<>();
        toExplore.add(entryPoint);
        while (!toExplore.isEmpty()) {
            BasicBlock block = toExplore.pop();
            if (seen.contains(block))
                continue;
            seen.add(block);
            toExplore.addAll(block.getSuccessors());
        }
        return new ArrayList<>(seen);
    }

    public static List<List<BasicBlock>> findStronglyConnectedComponents(BasicBlock entryPoint) {
        var basicBlocks = allNodes(entryPoint);
        correctPredecessors(entryPoint);
        return tarjan(basicBlocks);
    }

    private static List<List<BasicBlock>> tarjan(List<BasicBlock> blocks) {
        HashMap<BasicBlock, Integer> blockToLowLinkValue = new HashMap<>();
        HashMap<BasicBlock, Integer> blockToIndex = new HashMap<>();
        HashMap<BasicBlock, Boolean> blockOnStack = new HashMap<>();
        List<List<BasicBlock>> stronglyConnectedComponents = new ArrayList<>();

        var index = 0;
        var blocksToExplore = new Stack<BasicBlock>();
        for (BasicBlock block : blocks) {
            if (!blockToIndex.containsKey(block)) {
                index = strongConnect(block, blockToIndex, blockOnStack, blockToLowLinkValue, blocksToExplore, stronglyConnectedComponents, index);
            }
        }
        return stronglyConnectedComponents;

    }

    private static Integer strongConnect(BasicBlock block,
                                         HashMap<BasicBlock, Integer> blockToIndex,
                                         HashMap<BasicBlock, Boolean> blockOnStack,
                                         HashMap<BasicBlock, Integer> blockToLowLinkValue,
                                         Stack<BasicBlock> blocksToExplore,
                                         List<List<BasicBlock>> strongConnectedComponentsList,
                                         Integer index) {
        blockToIndex.put(block, index);
        blockToLowLinkValue.put(block, index);
        index = index + 1;
        blocksToExplore.push(block);
        blockOnStack.put(block, true);

        for (BasicBlock successor : block.getSuccessors()) {
            if (!blockToIndex.containsKey(successor)) {
                index = strongConnect(successor, blockToIndex, blockOnStack, blockToLowLinkValue, blocksToExplore, strongConnectedComponentsList, index);
                blockToLowLinkValue.put(block, Math.min(blockToLowLinkValue.get(block), blockToLowLinkValue.get(successor)));
            } else if (blockOnStack.get(successor)) {
                blockToLowLinkValue.put(block, Math.min(blockToLowLinkValue.get(block), blockToIndex.get(successor)));
            }
        }
        if (blockToLowLinkValue
                .get(block)
                .equals(blockToIndex.get(block))) {
            var stronglyConnectedComponent = new ArrayList<BasicBlock>();
            BasicBlock toAdd;
            do {
                toAdd = blocksToExplore.pop();
                blockOnStack.put(toAdd, false);
                stronglyConnectedComponent.add(toAdd);
            } while (!block.equals(toAdd));
            strongConnectedComponentsList.add(stronglyConnectedComponent);
        }
        return index;
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
