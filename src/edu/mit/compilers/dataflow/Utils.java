package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;


public class Utils {

     static void correctPredecessors(BasicBlock block) {
         List<BasicBlock> basicBlocks = allNodes(block);

         for (BasicBlock basicBlock : basicBlocks)
             basicBlock.getPredecessors().clear();

         for (BasicBlock basicBlock : basicBlocks) {
             for (BasicBlock successor: basicBlock.getSuccessors()) {
                 successor.addPredecessor(basicBlock);
             }
         }
     }

    static List<BasicBlock> allNodes(BasicBlock entryPoint) {
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
}
