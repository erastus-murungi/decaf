package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public class NopVisitor implements CFGVisitor<Void> {
    CFGBlock exit = new NOP();

    @Override
    public Void visit(CFGNonConditional cfgNonConditional, SymbolTable symbolTable) {
        // we assume this is the first instance of the
        if (cfgNonConditional.autoChild != null) {
            cfgNonConditional.autoChild.accept(this, symbolTable);
        }
        return null;
    }

    @Override
    public Void visit(CFGConditional cfgConditional, SymbolTable symbolTable) {
        if (cfgConditional.trueChild != null) {
            cfgConditional.trueChild.accept(this, symbolTable);
        }
        if (cfgConditional.falseChild != null) {
            cfgConditional.falseChild.accept(this, symbolTable);
        }
        return null;
    }

    @Override
    public Void visit(NOP nop, SymbolTable symbolTable) {
        CFGBlock endBlock = exit;
        if (nop.autoChild != null) {
            endBlock = nop.autoChild;
        }
        for (CFGBlock parent : nop.parents) {
            // connecting parents to child
            if (parent instanceof CFGConditional) {
                CFGConditional parentConditional = (CFGConditional) parent;
                if (parentConditional.trueChild == nop) {
                    parentConditional.trueChild = endBlock;
                } else if (parentConditional.falseChild == nop) {
                    parentConditional.falseChild = endBlock;
                }
            } else {
                CFGNonConditional parentNonConditional = (CFGNonConditional) parent;
                if (parentNonConditional.autoChild == nop) {
                    parentNonConditional.autoChild = endBlock;
                }
            }
            endBlock.parents.add(parent);
        }
        return null;
    }
}
