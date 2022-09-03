package edu.mit.compilers.cfg;

import java.util.HashMap;


public class MaximalBasicBlocksUtil {
    HashMap<BasicBlock, Integer> visited = new HashMap<>();
    public NOP exitNOP;

    public BasicBlock visit(BasicBlock basicBlock) {
        return switch (basicBlock.getBasicBlockType()) {
            case NO_BRANCH -> visitBranchLess(basicBlock);
            case BRANCH -> visitWithBranch(basicBlock);
            default -> visitNop((NOP) basicBlock);
        };
    }

    public BasicBlock visitBranchLess(BasicBlock basicBlockBranchLess) {
        // viewpoint of child
        if (!visited.containsKey(basicBlockBranchLess)) {
            visited.put(basicBlockBranchLess, 1);

            if (basicBlockBranchLess.getSuccessor() != null) {
                visit(basicBlockBranchLess.getSuccessor());
            }

            if (!basicBlockBranchLess.getSuccessor().hasBranch()) {
                if (basicBlockBranchLess.getSuccessor() == exitNOP)
                    return basicBlockBranchLess;
                BasicBlock child = basicBlockBranchLess.getSuccessor();
                BasicBlock grandChild = child.getSuccessor();

                basicBlockBranchLess.addAstNodes(child.getAstNodes());

                if (grandChild != null) {
                    grandChild.removePredecessor(basicBlockBranchLess.getSuccessor());
                    grandChild.addPredecessor(basicBlockBranchLess);
                }
                basicBlockBranchLess.setSuccessor(grandChild);
            } else {
                BasicBlock child = basicBlockBranchLess.getSuccessor();
                if (visited.get(child) == null || visited.get(child) == 1 || basicBlockBranchLess.isRoot()) {
                    // we should put our code into the conditional;
                    if (child != null) {
                        child.getAstNodes().addAll(0, basicBlockBranchLess.getAstNodes());
                        if (basicBlockBranchLess.isRoot()) {
                            return child;
                        }
                        for (BasicBlock parent : basicBlockBranchLess.getPredecessors()) {
                            if (parent.hasBranch()) {
                                if (basicBlockBranchLess == parent.getFalseTarget()) {
                                    parent.setFalseTarget(child);
                                } else {
                                    parent.setTrueTarget(child);
                                }
                            } else {
                                parent.setSuccessor(child);
                            }
                            child.removePredecessor(basicBlockBranchLess);
                            child.addPredecessors(basicBlockBranchLess.getPredecessors());
                        }
                    }
                }
            }
        }
        return basicBlockBranchLess;
    }

    public BasicBlock visitWithBranch(BasicBlock basicBlockWithBranch) {
        if (!visited.containsKey(basicBlockWithBranch)) {
            visited.put(basicBlockWithBranch, 1);
            basicBlockWithBranch.setTrueTarget(visit(basicBlockWithBranch.getTrueTarget()));
            basicBlockWithBranch.setFalseTarget(visit(basicBlockWithBranch.getFalseTarget()));
        }
        visited.put(basicBlockWithBranch, visited.get(basicBlockWithBranch) + 1);
        return basicBlockWithBranch;
    }

    public BasicBlock visitNop(NOP nop) {
        if (nop == exitNOP) {
            if (nop.getSuccessor() != null) {
                throw new IllegalStateException("expected exit NOP to have no child");
            }
            return nop;
        }
        BasicBlock block = nop.getSuccessor();
        while (block instanceof NOP blockNOP) {
            if (block == exitNOP)
                return blockNOP;
            block = blockNOP.getSuccessor();
        }
        if (block.hasNoBranch())
            return visit(block);
        return block;
    }
}
