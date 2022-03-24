package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public class MaximalVisitor implements CFGVisitor<Void> {
    @Override
    public Void visit(CFGBlock cfgBlock, SymbolTable symbolTable) {
        // viewpoint of child
        if (cfgBlock.parents.size() == 1 && cfgBlock.parents.get(0).autoChild == cfgBlock) {
            CFGBlock parent = cfgBlock.parents.get(0);
            parent.lines.addAll(cfgBlock.lines);
            // connecting parent to our children
            parent.autoChild = cfgBlock.autoChild;
            parent.trueChild = cfgBlock.trueChild;
            parent.falseChild = cfgBlock.falseChild;

            // connecting child to our parent
            if (cfgBlock.autoChild != null) {
                cfgBlock.autoChild.parents.remove(cfgBlock);
                cfgBlock.autoChild.parents.add(parent);
            }
            else {
                cfgBlock.trueChild.parents.remove(cfgBlock);
                cfgBlock.trueChild.parents.add(parent);
                cfgBlock.falseChild.parents.remove(cfgBlock);
                cfgBlock.falseChild.parents.add(parent);
            }
            // done modifying CFG

            // recurse on our children
            if (cfgBlock.autoChild != null) {
                cfgBlock.autoChild.accept(this, symbolTable);
            }
            else {
                cfgBlock.trueChild.accept(this, symbolTable);
                cfgBlock.falseChild.accept(this, symbolTable);
            }
        }

        return null;
    }

    @Override
    public Void visit(NOP nop, SymbolTable symbolTable) {
        return null;
    }
}
