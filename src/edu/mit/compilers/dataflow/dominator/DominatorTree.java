package edu.mit.compilers.dataflow.dominator;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.dataflow.Utils;

import java.util.*;

public class DominatorTree {

    public DominatorTree() {
    }


    private static BasicBlock preprocess(BasicBlock entryBlock) {
        var entry = new NOP("Entry");
        entry.autoChild = entryBlock;
        Utils.correctPredecessors(entry);
        return entry;
    }

    public static HashMap<BasicBlock, BasicBlock> immediateDominators(BasicBlock entryBlock) {
        return immediateDominatorsImpl(preprocess(entryBlock));
    }

    private static HashMap<BasicBlock, BasicBlock> immediateDominatorsImpl(BasicBlock entryBlock) {
        // compute reverse post-order traversal
        var rpo = reversePostOrder(entryBlock);

        // map each vertex to its reverse post-order traversal index
        var order = new HashMap<BasicBlock, Integer>();
        for (int i = 0; i < rpo.size(); i++)
            order.put(rpo.get(i), i);

        // create idom tree as map
        var idom = new HashMap<BasicBlock, BasicBlock>();
        idom.put(entryBlock, entryBlock);

        boolean changed = true;
        while (changed) {
            changed = false;

            // for each non-source vertex in reverse post-order
            for (BasicBlock b : rpo) {
                if (b.equals(entryBlock)) continue;

                // choose an already-processed predecessor (there must be one)
                var ps = b.getPredecessors();
                var processed = ps.stream().filter(idom::containsKey).findFirst();
                // the idom tree is built in reverse post-order to ensure already-processed predecessor(s) exist
                var fresh = processed.orElseThrow();

                // iteratively find non-empty, ancestral, intersection from predecessors
                for (BasicBlock p : ps) {
                    if (idom.containsKey(p)) {
                        var v1 = p;
                        var v2 = fresh;

                        // traverse partial idom tree in post-order to find (least) common ancestor
                        while (!order.get(v1).equals(order.get(v2))) {
                            // greater than because order is in reverse
                            while (order.get(v1) > order.get(v2)) v1 = idom.get(v1);
                            while (order.get(v2) > order.get(v1)) v2 = idom.get(v2);
                        }

                        fresh = v1;
                    }
                }

                // update idom entry for current vertex
                if (fresh != idom.get(b)) {
                    idom.put(b, fresh);
                    changed = true;
                }
            }
        }
        return idom;
    }


    public static HashMap<BasicBlock, List<BasicBlock>> dominatorTree(BasicBlock entryPoint) {
        var iDom = immediateDominators(entryPoint);
        var dominatorTree = new HashMap<BasicBlock, List<BasicBlock>>();
        for (var edge: iDom.entrySet()) {
            var block = edge.getKey();
            BasicBlock dominatorBlock = edge.getValue();
            BasicBlock currentBlock;

            List<BasicBlock> dominators = new ArrayList<>();

            do {
                dominators.add(dominatorBlock);
                currentBlock = dominatorBlock;
                dominatorBlock = iDom.get(currentBlock);
            } while (dominatorBlock != currentBlock);

            dominatorTree.put(block, dominators);
        }
        return dominatorTree;
    }



    public Map<BasicBlock, Set<BasicBlock>> dominanceFrontier(BasicBlock entryBlock) {
        entryBlock = preprocess(entryBlock);
        var basicBlocks = reversePostOrder(entryBlock);
        var iDom = immediateDominatorsImpl(entryBlock);
        var dominanceFrontier = new HashMap<BasicBlock, Set<BasicBlock>>();

        for (var basicBlock: basicBlocks) {
            var frontier = new HashSet<BasicBlock>();
            if (basicBlock.getPredecessors().size() >= 2) {
                for (var predecessor: basicBlock.getPredecessors()) {
                    var runner = predecessor;
                    while (runner != iDom.get(basicBlock)) {
                        // TODO: finish
                    }
                }
            }
        }
        return null;
    }


    public Set<BasicBlock> dominanceFrontier(BasicBlock basicBlock, Map<BasicBlock, Set<BasicBlock>> dominanceFrontier, Map<BasicBlock, BasicBlock> iDom) {
        if (dominanceFrontier.get(basicBlock) == null)
            return dominanceFrontier.get(basicBlock);

        var frontier = new HashSet<BasicBlock>();

        for (var successor: basicBlock.getSuccessors()) {
            for (var node: dominanceFrontier.get(successor)) {
                // TODO: finish
            }
        }

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
