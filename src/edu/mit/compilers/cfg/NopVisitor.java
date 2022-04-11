package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

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
    public Void visit(BasicBlockBranchLess basicBlockBranchLess, SymbolTable symbolTable) {
        if (!seen.contains(basicBlockBranchLess)) {
            seen.add(basicBlockBranchLess);
            // we assume this is the first instance of the
            if (basicBlockBranchLess.autoChild != null) {
                basicBlockBranchLess.autoChild.accept(this, symbolTable);
            }
        }
        return null;
    }

    @Override
    public Void visit(BasicBlockWithBranch basicBlockWithBranch, SymbolTable symbolTable) {
        if (!seen.contains(basicBlockWithBranch)) {
            seen.add(basicBlockWithBranch);
            if (basicBlockWithBranch.trueChild != null) {
                basicBlockWithBranch.trueChild.accept(this, symbolTable);
            }
            if (basicBlockWithBranch.falseChild != null) {
                basicBlockWithBranch.falseChild.accept(this, symbolTable);
            }
        }
        return null;
    }

    @Override
    public Void visit(NOP nop, SymbolTable symbolTable) {
        if (!seen.contains(nop)) {
            List<BasicBlock> parentsCopy = new ArrayList<>(nop.getPredecessors());
            seen.add(nop);
            BasicBlock endBlock;
            if (nop == exit) {
                nop.autoChild = null;
                return null;
            }
            if (nop.autoChild != null) {
                nop.autoChild.accept(this, symbolTable);
                endBlock = nop.autoChild;
            } else {
                endBlock = exit;
            }
            for (BasicBlock parent : parentsCopy) {
                // connecting parents to child
                if (parent instanceof BasicBlockWithBranch) {
                    BasicBlockWithBranch parentConditional = (BasicBlockWithBranch) parent;
                    if (parentConditional.trueChild == nop) {
                        parentConditional.trueChild = endBlock;
                    } else if (parentConditional.falseChild == nop) {
                        parentConditional.falseChild = endBlock;
                    }
                } else {
                    BasicBlockBranchLess parentNonConditional = (BasicBlockBranchLess) parent;
                    if (parentNonConditional.autoChild == nop) {
                        parentNonConditional.autoChild = endBlock;
                    }
                }
                endBlock.removePredecessor(nop);
                endBlock.addPredecessor(parent);
            }
        }
        return null;
    }
}
