package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public class NopVisitor implements CFGVisitor<Void> {
    CFGBlock exit = new NOP();

    @Override
    public Void visit(CFGBlock cfgBlock, SymbolTable symbolTable) {
        if (cfgBlock.autoChild != null) {
            cfgBlock.autoChild.accept(this, symbolTable);
        }
        if (cfgBlock.trueChild != null) {
            cfgBlock.trueChild.accept(this, symbolTable);
        }
        if (cfgBlock.falseChild != null) {
            cfgBlock.falseChild.accept(this, symbolTable);
        }
        return null;
    }

    @Override
    public Void visit(NOP nop, SymbolTable symbolTable) {
        CFGBlock endBlock = exit;
        if (nop.autoChild != null) {
            endBlock = nop.autoChild;
        }
        for (CFGBlock parent: nop.parents) {
            // connecting parents to child
            if (parent.autoChild == nop) {
                parent.autoChild = endBlock;
            }
            else if (parent.trueChild == nop) {
                parent.trueChild = endBlock;
            }
            else if (parent.falseChild == nop) {
                parent.falseChild = endBlock;
            }
            endBlock.parents.add(parent);
        }
        return null;
    }
}
