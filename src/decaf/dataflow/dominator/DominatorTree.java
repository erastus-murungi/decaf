package decaf.dataflow.dominator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import decaf.cfg.BasicBlock;
import decaf.cfg.NOP;
import decaf.common.StronglyConnectedComponentsTarjan;

public class DominatorTree extends HashMap<BasicBlock, BasicBlock> {
  /**
   * A map of a basic block to the set of nodes which dominate it
   */
  @NotNull
  private final Map<BasicBlock, List<BasicBlock>> dominators;
  @NotNull
  private final NOP entry;
  @NotNull
  private final Map<BasicBlock, Set<BasicBlock>> basicBlockToChildrenMap;
  @NotNull
  private final Map<BasicBlock, Set<BasicBlock>> basicBlockToDominanceFrontierMap;

  public DominatorTree(BasicBlock basicBlock) {
    entry = preprocess(basicBlock);
    immediateDominatorsImpl(entry);
    basicBlockToChildrenMap = computeChildren();
    dominators = computeBlockToDoms();
    basicBlockToDominanceFrontierMap = computeDominanceFrontier();
  }

  private static List<BasicBlock> reversePostOrder(@NotNull BasicBlock entryBlock) {
    var order = new ArrayList<BasicBlock>();
    postOrder(
        entryBlock,
        new HashSet<>(),
        order
    );
    Collections.reverse(order);
    return order;
  }

  private static void postOrder(
      @NotNull BasicBlock start,
      @NotNull Set<BasicBlock> visited,
      @NotNull List<BasicBlock> out
  ) {
    if (visited.contains(start)) return;

    visited.add(start);

    for (BasicBlock successor : start.getSuccessors())
      postOrder(
          successor,
          visited,
          out
      );

    out.add(start);
  }

  private static List<BasicBlock> postOrder(BasicBlock entryBlock) {
    var order = new ArrayList<BasicBlock>();
    postOrder(
        entryBlock,
        new HashSet<>(),
        order
    );
    return order;
  }

  @NotNull
  public Set<BasicBlock> getChildren(@NotNull BasicBlock basicBlock) {
    return basicBlockToChildrenMap.getOrDefault(
        basicBlock,
        Collections.emptySet()
    );
  }

  @NotNull
  private NOP preprocess(@NotNull BasicBlock entryBlock) {
    NOP entry = new NOP(
        "dom",
        NOP.NOPType.METHOD_ENTRY
    );
    entry.setSuccessor(entryBlock);
    StronglyConnectedComponentsTarjan.correctPredecessors(entry);
    return entry;
  }

  @NotNull
  public List<BasicBlock> getDominators(@NotNull BasicBlock basicBlock) {
    return dominators.get(basicBlock);
  }

  @NotNull
  public Set<BasicBlock> getDominanceFrontier(@NotNull BasicBlock basicBlock) {
    return basicBlockToDominanceFrontierMap.getOrDefault(
        basicBlock,
        Collections.emptySet()
    );
  }

  private void immediateDominatorsImpl(@NotNull BasicBlock entryBlock) {
    // compute reverse post-order traversal
    var rpo = reversePostOrder(entryBlock);

    // map each vertex to its reverse post-order traversal index
    var order = new HashMap<BasicBlock, Integer>();
    for (int i = 0; i < rpo.size(); i++)
      order.put(
          rpo.get(i),
          i
      );

    // create idom tree as map
    put(
        entryBlock,
        entryBlock
    );

    boolean changed = true;
    while (changed) {
      changed = false;

      // for each non-source vertex in reverse post-order
      for (BasicBlock b : rpo) {
        if (b.equals(entryBlock)) continue;

        // choose an already-processed predecessor (there must be one)
        var ps = b.getPredecessors();
        var processed = ps.stream()
                          .filter(this::containsKey)
                          .findFirst();
        // the idom tree is built in reverse post-order to ensure already-processed predecessor(s) exist
        var fresh = processed.orElseThrow();

        // iteratively find non-empty, ancestral, intersection from predecessors
        for (BasicBlock p : ps) {
          if (containsKey(p)) {
            var v1 = p;
            var v2 = fresh;

            // traverse partial idom tree in post-order to find (least) common ancestor
            while (!order.get(v1)
                         .equals(order.get(v2))) {
              // greater than because order is in reverse
              while (order.get(v1) > order.get(v2)) v1 = get(v1);
              while (order.get(v2) > order.get(v1)) v2 = get(v2);
            }

            fresh = v1;
          }
        }

        // update idom entry for current vertex
        if (fresh != get(b)) {
          put(
              b,
              fresh
          );
          changed = true;
        }
      }
    }
  }

  @NotNull
  public Map<BasicBlock, List<BasicBlock>> computeBlockToDoms() {
    var dominatorTree = new HashMap<BasicBlock, List<BasicBlock>>();
    for (var edge : entrySet()) {
      var block = edge.getKey();
      var dominatorBlock = edge.getValue();

      var dominators = new ArrayList<BasicBlock>();
      dominators.add(block);

      BasicBlock currentBlock;
      do {
        dominators.add(dominatorBlock);
        currentBlock = dominatorBlock;
        dominatorBlock = get(currentBlock);
      } while (dominatorBlock != currentBlock);

      dominatorTree.put(
          block,
          dominators
      );
    }
    return dominatorTree;
  }

  /**
   * a node d dominates a node n if every path from the entry node to n must go through d
   * Notationally, this is written as d dom n
   *
   * @param m a node in the dominator tree
   * @param n another node in the dominator tree
   * @return true if m dominates n and false otherwise
   * @throws IllegalArgumentException if either m or n is not the dominator tree
   */
  public boolean dom(
      @NotNull BasicBlock m,
      @NotNull BasicBlock n
  ) {
    if (!containsKey(m)) throw new IllegalArgumentException(m + " not found in tree");
    if (!containsKey(n)) throw new IllegalArgumentException(n + " not found in tree");
    return dominators.get(n)
                     .contains(m);
  }

  /**
   * A node d strictly dominates a node n if d dominates n and d does not equal n.
   *
   * @param m a node in the dominator tree
   * @param n another node in the dominator tree
   * @return true if m strictly dominates n and false otherwise
   * @throws IllegalArgumentException if either m or n is not the dominator tree
   */
  public boolean strictDom(
      BasicBlock m,
      BasicBlock n
  ) {
    return !m.equals(n) && dom(
        m,
        n
    );
  }

  public boolean isImmediateDom(
      BasicBlock m,
      BasicBlock n
  ) {
    return get(m).equals(n);
  }

  @NotNull
  private Map<BasicBlock, Set<BasicBlock>> computeChildren() {
    var basicBlockToChildrenMap = new HashMap<BasicBlock, Set<BasicBlock>>();
    for (BasicBlock B : keySet()) {
      var immediateDom = get(B);
      basicBlockToChildrenMap.putIfAbsent(
          immediateDom,
          new HashSet<>()
      );
      basicBlockToChildrenMap.get(immediateDom)
                             .add(B);
    }
    return basicBlockToChildrenMap;
  }

  @NotNull
  public Map<BasicBlock, Set<BasicBlock>> computeDominanceFrontier() {
    var dominanceFrontier = new HashMap<BasicBlock, Set<BasicBlock>>();
    computeDominanceFrontier(
        Objects.requireNonNull(entry.getSuccessor()),
        dominanceFrontier
    );
    return dominanceFrontier;
  }

  /**
   * The dominance frontier of a node d is the set of all nodes ni such that d dominates an immediate predecessor of ni,
   * but d does not strictly dominate ni. It is the set of nodes where d's dominance stops.
   *
   * @param basicBlock                       the node n
   * @param basicBlockToDominanceFrontierMap a map of a basic block to its dominance frontier
   */
  public void computeDominanceFrontier(
      @NotNull BasicBlock basicBlock,
      @NotNull Map<BasicBlock, Set<BasicBlock>> basicBlockToDominanceFrontierMap
  ) {
    var dominanceFrontier = new HashSet<BasicBlock>();
    for (var successor : basicBlock.getSuccessors()) {
      if (!get(successor).equals(basicBlock)) {
        dominanceFrontier.add(successor);
      }
    }
    final var children = basicBlockToChildrenMap.getOrDefault(
        basicBlock,
        Collections.emptySet()
    );
    for (var child : children) {
      computeDominanceFrontier(
          child,
          basicBlockToDominanceFrontierMap
      );
      for (var w : basicBlockToDominanceFrontierMap.get(child)) {
        if (!dom(
            w,
            basicBlock
        ) || basicBlock == w) {
          dominanceFrontier.add(w);
        }
      }
    }

    basicBlockToDominanceFrontierMap.put(
        basicBlock,
        dominanceFrontier
    );
  }

  public List<BasicBlock> preorder() {
    var Q = new ArrayDeque<BasicBlock>();
    var visited = new HashSet<BasicBlock>();
    var preorderList = new ArrayList<BasicBlock>();
    Q.add(entry);
    visited.add(entry);
    while (!Q.isEmpty()) {
      var v = Q.remove();
      preorderList.add(v);
      v.getSuccessors()
       .forEach(successor -> {
         if (!visited.contains(successor)) {
           Q.add(successor);
           visited.add(successor);
         }
       });
    }
    return preorderList;
  }
}
