package edu.mit.compilers.dataflow.dominator;

import static edu.mit.compilers.dataflow.analyses.DataFlowAnalysis.correctPredecessors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.utils.GraphVizPrinter;

import java.util.*;

public class ImmediateDominator extends HashMap<BasicBlock, BasicBlock> {
    public static final String ENTRY_BLOCK_LABEL = "Entry";

    /**
     * A map of a basic block to the set of nodes which dominate it
     */

    private final Map<BasicBlock, List<BasicBlock>> dominators;

    public ImmediateDominator(BasicBlock basicBlock) {
        immediateDominatorsImpl(basicBlock);
        dominators = computeBlockToDoms();
        if (CLI.debug)
            GraphVizPrinter.printDominatorTree(this);
    }


    private BasicBlock preprocess(BasicBlock entryBlock) {
        NOP entry = new NOP(ENTRY_BLOCK_LABEL);
        entry.autoChild = entryBlock;
        correctPredecessors(entry);
        return entry;
    }

    public List<BasicBlock> getDominators(BasicBlock basicBlock) {
        return dominators.get(basicBlock);
    }

    public void immediateDominators(BasicBlock entryBlock) {
        immediateDominatorsImpl(preprocess(entryBlock));
    }

    private void immediateDominatorsImpl(BasicBlock entryBlock) {
        // compute reverse post-order traversal
        var rpo = reversePostOrder(entryBlock);

        // map each vertex to its reverse post-order traversal index
        var order = new HashMap<BasicBlock, Integer>();
        for (int i = 0; i < rpo.size(); i++)
            order.put(rpo.get(i), i);

        // create idom tree as map
        put(entryBlock, entryBlock);

        boolean changed = true;
        while (changed) {
            changed = false;

            // for each non-source vertex in reverse post-order
            for (BasicBlock b : rpo) {
                if (b.equals(entryBlock)) continue;

                // choose an already-processed predecessor (there must be one)
                var ps = b.getPredecessors();
                var processed = ps.stream().filter(this::containsKey).findFirst();
                // the idom tree is built in reverse post-order to ensure already-processed predecessor(s) exist
                var fresh = processed.orElseThrow();

                // iteratively find non-empty, ancestral, intersection from predecessors
                for (BasicBlock p : ps) {
                    if (containsKey(p)) {
                        var v1 = p;
                        var v2 = fresh;

                        // traverse partial idom tree in post-order to find (least) common ancestor
                        while (!order.get(v1).equals(order.get(v2))) {
                            // greater than because order is in reverse
                            while (order.get(v1) > order.get(v2)) v1 = get(v1);
                            while (order.get(v2) > order.get(v1)) v2 = get(v2);
                        }

                        fresh = v1;
                    }
                }

                // update idom entry for current vertex
                if (fresh != get(b)) {
                    put(b, fresh);
                    changed = true;
                }
            }
        }
    }


    public Map<BasicBlock, List<BasicBlock>> computeBlockToDoms() {
        var dominatorTree = new HashMap<BasicBlock, List<BasicBlock>>();
        for (var edge : entrySet()) {
            var block = edge.getKey();
            var dominatorBlock = edge.getValue();

            var dominators = new ArrayList<BasicBlock>();

            BasicBlock currentBlock;
            do {
                dominators.add(dominatorBlock);
                currentBlock = dominatorBlock;
                dominatorBlock = get(currentBlock);
            } while (dominatorBlock != currentBlock);

            dominatorTree.put(block, dominators);
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
    public boolean dom(BasicBlock m, BasicBlock n) {
        if (!containsKey(m))
            throw new IllegalArgumentException(m + " not found in tree");
        if (!containsKey(n))
            throw new IllegalArgumentException(n + " not found in tree");
        return dominators.get(m).contains(n);
    }

    /**
     * A node d strictly dominates a node n if d dominates n and d does not equal n.
     *
     * @param m a node in the dominator tree
     * @param n another node in the dominator tree
     * @return true if m strictly dominates n and false otherwise
     * @throws IllegalArgumentException if either m or n is not the dominator tree
     */
    public boolean strictDom(BasicBlock m, BasicBlock n) {
        return !m.equals(n) && dom(m, n);
    }

    public boolean isImmediateDom(BasicBlock m, BasicBlock n) {
        return get(m).equals(n);
    }

    public Map<BasicBlock, Set<BasicBlock>> computeDominanceFrontier(BasicBlock entryBlock) {
        var dominanceFrontier = new HashMap<BasicBlock, Set<BasicBlock>>();
        computeDominanceFrontier(entryBlock, dominanceFrontier);
        return dominanceFrontier;
    }

    /**
     * The dominance frontier of a node d is the set of all nodes ni such that d dominates an immediate predecessor of ni,
     * but d does not strictly dominate ni. It is the set of nodes where d's dominance stops.
     *
     * @param basicBlock the node n
     * @param dominanceFrontier a map of a block to a dominance frontier
     *
     * @return the dominance frontier of basicBlock
     */
    public Set<BasicBlock> computeDominanceFrontier(BasicBlock basicBlock, Map<BasicBlock, Set<BasicBlock>> dominanceFrontier) {
        if (dominanceFrontier.get(basicBlock) == null)
            return dominanceFrontier.get(basicBlock);

        var frontier = new HashSet<BasicBlock>();

        for (var child : dominators.get(basicBlock)) {
            for (var node : dominanceFrontier.get(child)) {
                if (!strictDom(basicBlock, node))
                    frontier.add(node);
            }
            for (var successor : basicBlock.getSuccessors()) {
                if (!strictDom(basicBlock, successor))
                    frontier.add(successor);
            }
        }
        dominanceFrontier.put(basicBlock, frontier);
        return frontier;
    }

    private static List<BasicBlock> reversePostOrder(BasicBlock entryBlock) {
        var order = new ArrayList<BasicBlock>();
        postOrder(entryBlock, new HashSet<>(), order);
        Collections.reverse(order);
        return order;
    }

    private static void postOrder(BasicBlock start, Set<BasicBlock> visited, List<BasicBlock> out) {
        if (visited.contains(start))
            return;

        visited.add(start);

        for (BasicBlock successor : start.getSuccessors())
            postOrder(successor, visited, out);

        out.add(start);
    }

    private static List<BasicBlock> postOrder(BasicBlock entryBlock) {
        var order = new ArrayList<BasicBlock>();
        postOrder(entryBlock, new HashSet<>(), order);
        return order;
    }
}
