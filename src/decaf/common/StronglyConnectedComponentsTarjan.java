package decaf.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import decaf.cfg.BasicBlock;

public class StronglyConnectedComponentsTarjan {
  public static List<BasicBlock> getReversePostOrder(BasicBlock entryPoint) {
    var stronglyConnectedComponents = findStronglyConnectedComponents(entryPoint);
    Collections.reverse(stronglyConnectedComponents);
    return stronglyConnectedComponents.stream()
                                      .flatMap(Collection::stream)
                                      .collect(Collectors.toList());
  }

  public static void correctPredecessors(BasicBlock block) {
    var basicBlocks = allBasicBlocks(block);

    for (var basicBlock : basicBlocks)
      basicBlock.getPredecessors()
                .clear();

    for (var basicBlock : basicBlocks) {
      for (var successor : basicBlock.getSuccessors()) {
        successor.addPredecessor(basicBlock);
      }
    }
  }

  public static List<BasicBlock> allBasicBlocks(@NotNull BasicBlock entryPoint) {
    var seen = new HashSet<BasicBlock>();
    var toExplore = new Stack<BasicBlock>();
    toExplore.add(entryPoint);
    while (!toExplore.isEmpty()) {
      BasicBlock block = toExplore.pop();
      if (seen.contains(block)) continue;
      seen.add(block);
      toExplore.addAll(block.getSuccessors());
    }
    return new ArrayList<>(seen);
  }

  public static List<List<BasicBlock>> findStronglyConnectedComponents(BasicBlock entryPoint) {
    var basicBlocks = allBasicBlocks(entryPoint);
    correctPredecessors(entryPoint);
    return tarjan(basicBlocks);
  }

  private static List<List<BasicBlock>> tarjan(List<BasicBlock> blocks) {
    var blockToLowLinkValue = new HashMap<BasicBlock, Integer>();
    var blockToIndex = new HashMap<BasicBlock, Integer>();
    var blockOnStack = new HashMap<BasicBlock, Boolean>();
    var stronglyConnectedComponents = new ArrayList<List<BasicBlock>>();

    var index = 0;
    var blocksToExplore = new Stack<BasicBlock>();
    for (var block : blocks) {
      if (!blockToIndex.containsKey(block)) {
        index = strongConnect(
            block,
            blockToIndex,
            blockOnStack,
            blockToLowLinkValue,
            blocksToExplore,
            stronglyConnectedComponents,
            index
        );
      }
    }
    return stronglyConnectedComponents;

  }

  private static Integer strongConnect(
      BasicBlock block,
      HashMap<BasicBlock, Integer> blockToIndex,
      HashMap<BasicBlock, Boolean> blockOnStack,
      HashMap<BasicBlock, Integer> blockToLowLinkValue,
      Stack<BasicBlock> blocksToExplore,
      List<List<BasicBlock>> strongConnectedComponentsList,
      Integer index
  ) {
    blockToIndex.put(
        block,
        index
    );
    blockToLowLinkValue.put(
        block,
        index
    );
    index = index + 1;
    blocksToExplore.push(block);
    blockOnStack.put(
        block,
        true
    );

    for (BasicBlock successor : block.getSuccessors()) {
      if (!blockToIndex.containsKey(successor)) {
        index = strongConnect(
            successor,
            blockToIndex,
            blockOnStack,
            blockToLowLinkValue,
            blocksToExplore,
            strongConnectedComponentsList,
            index
        );
        blockToLowLinkValue.put(
            block,
            Math.min(
                blockToLowLinkValue.get(block),
                blockToLowLinkValue.get(successor)
            )
        );
      } else if (blockOnStack.get(successor)) {
        blockToLowLinkValue.put(
            block,
            Math.min(
                blockToLowLinkValue.get(block),
                blockToIndex.get(successor)
            )
        );
      }
    }
    if (blockToLowLinkValue.get(block)
                           .equals(blockToIndex.get(block))) {
      var stronglyConnectedComponent = new ArrayList<BasicBlock>();
      BasicBlock toAdd;
      do {
        toAdd = blocksToExplore.pop();
        blockOnStack.put(
            toAdd,
            false
        );
        stronglyConnectedComponent.add(toAdd);
      } while (!block.equals(toAdd));
      strongConnectedComponentsList.add(stronglyConnectedComponent);
    }
    return index;
  }
}
