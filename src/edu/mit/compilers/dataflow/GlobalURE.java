package edu.mit.compilers.dataflow;

import java.util.HashSet;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;

public class GlobalURE {
    private static void run(BasicBlock entryBlock,  Set<BasicBlock> visited) {

    }


    private static void run(BasicBlock entryBlock) {
        var visited = new HashSet<BasicBlock>();

        run(entryBlock, visited);

        for (BasicBlock basicBlock: DataFlowAnalysis.getReversePostOrder(entryBlock)) {
            if (!visited.contains(basicBlock)) {

            }
        }
    }
}
