package edu.mit.compilers.cfg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NopVisitor implements BasicBlockVisitor<Void> {
    NOP exit;
    final Set<BasicBlock> seen;

    public NopVisitor() {
        seen = new HashSet<>();
    }

    @Override
    public Void visit(BasicBlockBranchLess basicBlockBranchLess) {
        if (!seen.contains(basicBlockBranchLess)) {
            seen.add(basicBlockBranchLess);
            // we assume this is the first instance of the
            if (basicBlockBranchLess.getSuccessor() != null) {
                basicBlockBranchLess.getSuccessor().accept(this);
            }
        }
        return null;
    }

    @Override
    public Void visit(BasicBlockWithBranch basicBlockWithBranch) {
        if (!seen.contains(basicBlockWithBranch)) {
            seen.add(basicBlockWithBranch);
            if (basicBlockWithBranch.getTrueTarget() != null) {
                basicBlockWithBranch.getTrueTarget()
                                    .accept(this);
            }
            if (basicBlockWithBranch.getFalseTarget() != null) {
                basicBlockWithBranch.getFalseTarget()
                                    .accept(this);
            }
        }
        return null;
    }

    @Override
    public Void visit(NOP nop) {
        if (!seen.contains(nop)) {
            List<BasicBlock> parentsCopy = new ArrayList<>(nop.getPredecessors());
            seen.add(nop);
            BasicBlock endBlock;
            if (nop == exit) {
                nop.setSuccessor(null);
                return null;
            }
            if (nop.getSuccessor() != null) {
                nop.getSuccessor().accept(this);
                endBlock = nop.getSuccessor();
            } else {
                endBlock = exit;
            }
            for (BasicBlock parent : parentsCopy) {
                // connecting parents to child
                if (parent instanceof BasicBlockWithBranch) {
                    BasicBlockWithBranch parentConditional = (BasicBlockWithBranch) parent;
                    if (parentConditional.getTrueTarget() == nop) {
                        parentConditional.setTrueTarget(endBlock);
                    } else if (parentConditional.getFalseTarget() == nop) {
                        parentConditional.setFalseTarget(endBlock);
                    }
                } else {
                    BasicBlockBranchLess parentNonConditional = (BasicBlockBranchLess) parent;
                    if (parentNonConditional.getSuccessor() == nop) {
                        parentNonConditional.setSuccessor(endBlock);
                    }
                }
                endBlock.removePredecessor(nop);
                endBlock.addPredecessor(parent);
            }
        }
        return null;
    }
}
