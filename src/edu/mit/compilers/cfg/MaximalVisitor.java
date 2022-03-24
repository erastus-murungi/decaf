package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public class MaximalVisitor implements CFGVisitor<Void> {
    @Override
    public Void visit(CFGNonConditional cfgNonConditional, SymbolTable symbolTable) {
                // viewpoint of child
        if (cfgNonConditional.parents.size() == 1 && cfgNonConditional.parents.get(0) instanceof CFGNonConditional) {
            CFGNonConditional parent = (CFGNonConditional) cfgNonConditional.parents.get(0);
            parent.lines.addAll(cfgNonConditional.lines);
            parent.autoChild = cfgNonConditional.autoChild;

            // connecting child to our parent
            if (cfgNonConditional.autoChild != null) {
                cfgNonConditional.autoChild.parents.remove(cfgNonConditional);
                cfgNonConditional.autoChild.parents.add(parent);
            }
            // done modifying CFG

            // recurse on our children
            if (cfgNonConditional.autoChild != null) {
                cfgNonConditional.autoChild.accept(this, symbolTable);
            }
        }
        return null;
    }

    @Override
    public Void visit(CFGConditional cfgConditional, SymbolTable symbolTable) {
                // viewpoint of child
        if (cfgConditional.parents.size() == 1 && cfgConditional.parents.get(0) instanceof CFGNonConditional) {
            CFGNonConditional parent = (CFGNonConditional) cfgConditional.parents.get(0);
            parent.lines.addAll(cfgConditional.lines);
            // connecting parent to our children
//            parent.trueChild = cfgConditional.trueChild;
//            parent.falseChild = cfgConditional.falseChild;

//            // connecting child to our parent
//            if (cfgBlock.autoChild != null) {
//                cfgBlock.autoChild.parents.remove(cfgBlock);
//                cfgBlock.autoChild.parents.add(parent);
//            }
//            else {
            cfgConditional.trueChild.parents.remove(cfgConditional);
            cfgConditional.trueChild.parents.add(parent);
            cfgConditional.falseChild.parents.remove(cfgConditional);
            cfgConditional.falseChild.parents.add(parent);
//            }
            // done modifying CFG

            // recurse on our children
//            if (cfgBlock.autoChild != null) {
//                cfgBlock.autoChild.accept(this, symbolTable);
//            }
//            else {
            cfgConditional.trueChild.accept(this, symbolTable);
            cfgConditional.falseChild.accept(this, symbolTable);
//            }
        }
        return null;
    }

    @Override
    public Void visit(NOP nop, SymbolTable symbolTable) {
        return null;
    }
}
