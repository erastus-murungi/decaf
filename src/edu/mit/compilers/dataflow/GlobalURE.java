package edu.mit.compilers.dataflow;

import java.util.HashSet;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.utils.TarjanSCC;

public class GlobalURE {
    private static void run(BasicBlock entryBlock, Set<BasicBlock> visited) {

    }


    private static void run(BasicBlock entryBlock) {
        var visited = new HashSet<BasicBlock>();

        run(entryBlock, visited);

        for (BasicBlock basicBlock : TarjanSCC.getReversePostOrder(entryBlock)) {
            if (!visited.contains(basicBlock)) {

            }
        }
    }
}
