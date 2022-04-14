package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;

import java.util.*;
import java.util.stream.Collectors;

public abstract class DataFlowAnalysis<Value> {

    public abstract Value meet(Collection<Value> domainElements);

    public abstract Value transferFunction(Value domainElement);

    public abstract Direction direction();

    public abstract Value initializer();

    public abstract Iterator<Value> order();

    public abstract void runWorkList();

    public static List<BasicBlock> getReversePostOrder(BasicBlock entryPoint) {
        List<List<BasicBlock>> stronglyConnectedComponents = findStronglyConnectedComponents(entryPoint);
        Collections.reverse(stronglyConnectedComponents);
        return stronglyConnectedComponents.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static List<List<BasicBlock>> findStronglyConnectedComponents(BasicBlock entryPoint) {
        var basicBlocks = Utils.allNodes(entryPoint);
        Utils.correctPredecessors(entryPoint);
        return tarjan(basicBlocks);
    }

    private static List<List<BasicBlock>> tarjan(List<BasicBlock> blocks) {
        HashMap<BasicBlock, Integer> blockToLowLinkValue = new HashMap<>();
        HashMap<BasicBlock, Integer> blockToIndex = new HashMap<>();
        HashMap<BasicBlock, Boolean> blockOnStack = new HashMap<>();
        List<List<BasicBlock>> stronglyConnectedComponents = new ArrayList<>();

        var index = 0;
        var blocksToExplore = new Stack<BasicBlock>();
        for (BasicBlock block: blocks) {
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

        for (BasicBlock successor: block.getSuccessors()) {
            if (!blockToIndex.containsKey(successor)) {
                index = strongConnect(successor, blockToIndex, blockOnStack, blockToLowLinkValue, blocksToExplore, strongConnectedComponentsList, index);
                blockToLowLinkValue.put(block, Math.min(blockToLowLinkValue.get(block), blockToLowLinkValue.get(successor)));
            } else if (blockOnStack.get(successor)) {
                blockToLowLinkValue.put(block, Math.min(blockToLowLinkValue.get(block), blockToIndex.get(successor)));
            }
        }
        if (blockToLowLinkValue.get(block).equals(blockToIndex.get(block))) {
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

    public static Map<ThreeAddressCode, Integer> getTacToPosMapping(ThreeAddressCodeList threeAddressCodeList) {
        var tacToPositionInList = new LinkedHashMap<ThreeAddressCode, Integer>();
        var index = 0;
        for (ThreeAddressCode tac: threeAddressCodeList) {
            tacToPositionInList.put(tac, index);
            ++index;
        }
        return tacToPositionInList;
    }
}
