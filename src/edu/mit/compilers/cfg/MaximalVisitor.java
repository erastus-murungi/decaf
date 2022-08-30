package edu.mit.compilers.cfg;

import java.util.HashMap;


public class MaximalVisitor implements BasicBlockVisitor<BasicBlock> {
    HashMap<BasicBlock, Integer> visited = new HashMap<>();
    public NOP exitNOP;

    @Override
    public BasicBlock visit(BasicBlockBranchLess basicBlockBranchLess) {
        // viewpoint of child
        if (!visited.containsKey(basicBlockBranchLess)) {
            visited.put(basicBlockBranchLess, 1);

            if (basicBlockBranchLess.getSuccessor() != null) {
                basicBlockBranchLess.getSuccessor().accept(this);
            }

            if (basicBlockBranchLess.getSuccessor() instanceof BasicBlockBranchLess) {
                if (basicBlockBranchLess.getSuccessor() == exitNOP)
                    return basicBlockBranchLess;
                BasicBlockBranchLess child = (BasicBlockBranchLess) basicBlockBranchLess.getSuccessor();
                BasicBlock grandChild = child.getSuccessor();

                basicBlockBranchLess.addAstNodes(child.getAstNodes());

                if (grandChild != null) {
                    grandChild.removePredecessor(basicBlockBranchLess.getSuccessor());
                    grandChild.addPredecessor(basicBlockBranchLess);
                }
                basicBlockBranchLess.setSuccessor(grandChild);
            } else {
                BasicBlockWithBranch child = (BasicBlockWithBranch) basicBlockBranchLess.getSuccessor();
                if (visited.get(child) == null || visited.get(child) == 1 || basicBlockBranchLess.isRoot()) {
                    // we should put our code into the conditional;
                    if (child != null) {
                        child.getAstNodes().addAll(0, basicBlockBranchLess.getAstNodes());
                        if (basicBlockBranchLess.isRoot()) {
                            return child;
                        }
                        for (BasicBlock parent : basicBlockBranchLess.getPredecessors()) {
                            if (parent instanceof BasicBlockWithBranch) {
                                if (basicBlockBranchLess == ((BasicBlockWithBranch) parent).getFalseTarget()) {
                                    ((BasicBlockWithBranch) parent).setFalseTarget(child);
                                } else {
                                    ((BasicBlockWithBranch) parent).setTrueTarget(child);
                                }
                            } else {
                                ((BasicBlockBranchLess) parent).setSuccessor(child);
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

    @Override
    public BasicBlock visit(BasicBlockWithBranch basicBlockWithBranch) {
        if (!visited.containsKey(basicBlockWithBranch)) {
            visited.put(basicBlockWithBranch, 1);
            basicBlockWithBranch.setTrueTarget(basicBlockWithBranch.getTrueTarget()
                                                                   .accept(this));
            basicBlockWithBranch.setFalseTarget(basicBlockWithBranch.getFalseTarget()
                                                                    .accept(this));
        }
        visited.put(basicBlockWithBranch, visited.get(basicBlockWithBranch) + 1);
        return basicBlockWithBranch;
    }

    @Override
    public BasicBlock visit(NOP nop) {
        if (nop == exitNOP) {
            if (nop.getSuccessor() != null) {
                throw new IllegalStateException("expected exit NOP to have no child");
            }
            return nop;
        }
        BasicBlock block = nop.getSuccessor();
        while (block instanceof NOP) {
            NOP blockNOP = ((NOP) block);
            if (block == exitNOP)
                return blockNOP;
            block = blockNOP.getSuccessor();
        }
        if (block instanceof BasicBlockBranchLess)
            return visit((BasicBlockBranchLess) block);
        return block;
    }
}
