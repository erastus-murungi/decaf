package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.HashMap;


public class MaximalVisitor implements BasicBlockVisitor<BasicBlock> {
    HashMap<BasicBlock, Integer> visited = new HashMap<>();
    public NOP exitNOP;

    @Override
    public BasicBlock visit(BasicBlockBranchLess basicBlockBranchLess, SymbolTable symbolTable) {
        // viewpoint of child
        if (!visited.containsKey(basicBlockBranchLess)) {
            visited.put(basicBlockBranchLess, 1);

            if (basicBlockBranchLess.autoChild != null) {
                basicBlockBranchLess.autoChild.accept(this, symbolTable);
            }

            if (basicBlockBranchLess.autoChild instanceof BasicBlockBranchLess) {
                if (basicBlockBranchLess.autoChild == exitNOP)
                    return basicBlockBranchLess;
                BasicBlockBranchLess child = (BasicBlockBranchLess) basicBlockBranchLess.autoChild;
                BasicBlock grandChild = child.autoChild;

                basicBlockBranchLess.lines.addAll(child.lines);

                if (grandChild != null) {
                    grandChild.removePredecessor(basicBlockBranchLess.autoChild);
                    grandChild.addPredecessor(basicBlockBranchLess);
                }
                basicBlockBranchLess.autoChild = grandChild;
            } else {
                BasicBlockWithBranch child = (BasicBlockWithBranch) basicBlockBranchLess.autoChild;
                if (visited.get(child) == null || visited.get(child) == 1 || basicBlockBranchLess.isRoot()) {
                    // we should put our code into the conditional;
                    if (child != null) {
                        child.lines.addAll(0, basicBlockBranchLess.lines);
                        if (basicBlockBranchLess.isRoot()) {
                            return child;
                        }
                        for (BasicBlock parent : basicBlockBranchLess.getPredecessors()) {
                            if (parent instanceof BasicBlockWithBranch) {
                                if (basicBlockBranchLess == ((BasicBlockWithBranch) parent).falseChild) {
                                    ((BasicBlockWithBranch) parent).falseChild = child;
                                } else {
                                    ((BasicBlockWithBranch) parent).trueChild = child;
                                }
                            } else {
                                ((BasicBlockBranchLess) parent).autoChild = child;
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
    public BasicBlock visit(BasicBlockWithBranch basicBlockWithBranch, SymbolTable symbolTable) {
        if (!visited.containsKey(basicBlockWithBranch)) {
            visited.put(basicBlockWithBranch, 1);
            basicBlockWithBranch.trueChild = basicBlockWithBranch.trueChild.accept(this, symbolTable);
            basicBlockWithBranch.falseChild = basicBlockWithBranch.falseChild.accept(this, symbolTable);
        }
        visited.put(basicBlockWithBranch, visited.get(basicBlockWithBranch) + 1);
        return basicBlockWithBranch;
    }

    @Override
    public BasicBlock visit(NOP nop, SymbolTable symbolTable) {
        if (nop == exitNOP) {
            if (nop.autoChild != null) {
                throw new IllegalStateException("expected exit NOP to have no child");
            }
            return nop;
        }
        BasicBlock block = nop.autoChild;
        while (block instanceof NOP) {
            NOP blockNOP = ((NOP) block);
            if (block == exitNOP)
                return blockNOP;
            block = blockNOP.autoChild;
        }
        if (block instanceof BasicBlockBranchLess)
            return visit((BasicBlockBranchLess) block, symbolTable);
        return block;
    }
}
