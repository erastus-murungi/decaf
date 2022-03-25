package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NopVisitor implements CFGVisitor<Void> {
    CFGBlock exit;
    final Set<CFGBlock> seen;
    final List<CFGBlock> order = new ArrayList<>();

    public NopVisitor() {
        seen = new HashSet<>();
    }

    @Override
    public Void visit(CFGNonConditional cfgNonConditional, SymbolTable symbolTable) {
        if (!seen.contains(cfgNonConditional)) {
            seen.add(cfgNonConditional);
            order.add(cfgNonConditional);
            // we assume this is the first instance of the
            if (cfgNonConditional.autoChild != null) {
                cfgNonConditional.autoChild.accept(this, symbolTable);
            }
        }
        return null;
    }

    @Override
    public Void visit(CFGConditional cfgConditional, SymbolTable symbolTable) {
        if (!seen.contains(cfgConditional)) {
            seen.add(cfgConditional);
            order.add(cfgConditional);
            if (cfgConditional.trueChild != null) {
                cfgConditional.trueChild.accept(this, symbolTable);
            }
            if (cfgConditional.falseChild != null) {
                cfgConditional.falseChild.accept(this, symbolTable);
            }
        }
        return null;
    }

    @Override
    public Void visit(NOP nop, SymbolTable symbolTable) {
        if (!seen.contains(nop)) {
            List<CFGBlock> parentsCopy = new ArrayList<>(nop.parents);
            seen.add(nop);
            order.add(nop);
            CFGBlock endBlock = exit;
            if (nop.autoChild != null) {
                nop.autoChild.accept(this, symbolTable);
                endBlock = nop.autoChild;
            }
            for (CFGBlock parent : parentsCopy) {
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
                endBlock.parents.remove(nop);
                endBlock.parents.add(parent);
            }
        }
        return null;
    }
}
