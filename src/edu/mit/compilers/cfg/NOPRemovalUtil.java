package edu.mit.compilers.cfg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NOPRemovalUtil {
    NOP exit;
    final Set<BasicBlock> seen;

    public NOPRemovalUtil() {
        seen = new HashSet<>();
    }

    public Void visit(BasicBlock basicBlock) {
        return switch (basicBlock.getBasicBlockType()) {
            case NO_BRANCH -> visitBasicBlockBranchLess(basicBlock);
            case BRANCH -> visitBasicBlockWithBranch(basicBlock);
            default -> visitNOP((NOP) basicBlock);
        };
    }

    public Void visitBasicBlockBranchLess(BasicBlock basicBlockBranchLess) {
        if (!seen.contains(basicBlockBranchLess)) {
            seen.add(basicBlockBranchLess);
            // we assume this is the first instance of the
            if (basicBlockBranchLess.getSuccessor() != null) {
                visit(basicBlockBranchLess.getSuccessor());
            }
        }
        return null;
    }

    public Void visitBasicBlockWithBranch(BasicBlock basicBlockWithBranch) {
        if (!seen.contains(basicBlockWithBranch)) {
            seen.add(basicBlockWithBranch);
            if (basicBlockWithBranch.getTrueTarget() != null) {
                visit(basicBlockWithBranch.getTrueTarget());
            }
            if (basicBlockWithBranch.getFalseTarget() != null) {
                visit(basicBlockWithBranch.getFalseTarget());
            }
        }
        return null;
    }

    public Void visitNOP(NOP nop) {
        if (!seen.contains(nop)) {
            List<BasicBlock> parentsCopy = new ArrayList<>(nop.getPredecessors());
            seen.add(nop);
            BasicBlock endBlock;
            if (nop == exit) {
                nop.setSuccessor(null);
                return null;
            }
            if (nop.getSuccessor() != null) {
                visit(nop.getSuccessor());
                endBlock = nop.getSuccessor();
            } else {
                endBlock = exit;
            }
            for (BasicBlock parent : parentsCopy) {
                // connecting parents to child
                if (parent.hasBranch()) {
                    if (parent.getTrueTarget() == nop) {
                        parent.setTrueTarget(endBlock);
                    } else if (parent.getFalseTarget() == nop) {
                        parent.setFalseTarget(endBlock);
                    }
                } else {
                    if (parent.getSuccessor() == nop) {
                        parent.setSuccessor(endBlock);
                    }
                }
                endBlock.removePredecessor(nop);
                endBlock.addPredecessor(parent);
            }
        }
        return null;
    }
}
