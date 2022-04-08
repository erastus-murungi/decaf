package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.HashMap;


public class MaximalVisitor implements CFGVisitor<CFGBlock> {
    HashMap<CFGBlock, Integer> visited = new HashMap<>();
    public NOP exitNOP;

    @Override
    public CFGBlock visit(CFGNonConditional cfgNonConditional, SymbolTable symbolTable) {
        // viewpoint of child
        if (!visited.containsKey(cfgNonConditional)) {
            visited.put(cfgNonConditional, 1);

            if (cfgNonConditional.autoChild != null) {
                cfgNonConditional.autoChild.accept(this, symbolTable);
            }

            if (cfgNonConditional.autoChild instanceof CFGNonConditional) {
                if (cfgNonConditional.autoChild == exitNOP)
                    return cfgNonConditional;
                CFGNonConditional child = (CFGNonConditional) cfgNonConditional.autoChild;
                CFGBlock grandChild = child.autoChild;

                cfgNonConditional.lines.addAll(child.lines);

                if (grandChild != null) {
                    grandChild.removePredecessor(cfgNonConditional.autoChild);
                    grandChild.addPredecessor(cfgNonConditional);
                }
                cfgNonConditional.autoChild = grandChild;
            } else {
                CFGConditional child = (CFGConditional) cfgNonConditional.autoChild;
                if (visited.get(child) == null || visited.get(child) == 1 || cfgNonConditional.isRoot()) {
                    // we should put our code into the conditional;
                    if (child != null) {
                        child.lines.addAll(0, cfgNonConditional.lines);
                        if (cfgNonConditional.isRoot()) {
                            return child;
                        }
                        for (CFGBlock parent : cfgNonConditional.getPredecessors()) {
                            if (parent instanceof CFGConditional) {
                                if (cfgNonConditional == ((CFGConditional) parent).falseChild) {
                                    ((CFGConditional) parent).falseChild = child;
                                } else {
                                    ((CFGConditional) parent).trueChild = child;
                                }
                            } else {
                                ((CFGNonConditional) parent).autoChild = child;
                            }
                            child.removePredecessor(cfgNonConditional);
                            child.addPredecessors(cfgNonConditional.getPredecessors());
                        }
                    }
                }
            }
        }
        return cfgNonConditional;
    }

    @Override
    public CFGBlock visit(CFGConditional cfgConditional, SymbolTable symbolTable) {
        if (!visited.containsKey(cfgConditional)) {
            visited.put(cfgConditional, 1);
            cfgConditional.trueChild = cfgConditional.trueChild.accept(this, symbolTable);
            cfgConditional.falseChild = cfgConditional.falseChild.accept(this, symbolTable);
        }
        visited.put(cfgConditional, visited.get(cfgConditional) + 1);
        return cfgConditional;
    }

    @Override
    public CFGBlock visit(NOP nop, SymbolTable symbolTable) {
        if (nop == exitNOP) {
            if (nop.autoChild != null) {
                throw new IllegalStateException("expected exit NOP to have no child");
            }
            return nop;
        }
        CFGBlock block = nop.autoChild;
        while (block instanceof NOP) {
            NOP blockNOP = ((NOP) block);
            if (block == exitNOP)
                return blockNOP;
            block = blockNOP.autoChild;
        }
        if (block instanceof CFGNonConditional)
            return visit((CFGNonConditional) block, symbolTable);
        return block;
    }
}
