package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.HashMap;


public class MaximalVisitor implements CFGVisitor<CFGBlock> {
    HashMap<CFGBlock, Integer> visited = new HashMap<>();

    @Override
    public CFGBlock visit(CFGNonConditional cfgNonConditional, SymbolTable symbolTable) {
        // viewpoint of child
        if (!visited.containsKey(cfgNonConditional)) {
            visited.put(cfgNonConditional, 1);

            if (cfgNonConditional.autoChild != null) {
                cfgNonConditional.autoChild.accept(this, symbolTable);
            }

            if (cfgNonConditional.autoChild instanceof CFGNonConditional) {
                CFGNonConditional child = (CFGNonConditional) cfgNonConditional.autoChild;
                CFGBlock grandChild = child.autoChild;

                cfgNonConditional.lines.addAll(child.lines);

                if (grandChild != null) {
                    grandChild.parents.remove(cfgNonConditional.autoChild);
                    grandChild.parents.add(cfgNonConditional);
                }
                cfgNonConditional.autoChild = grandChild;
            } else {
                CFGConditional child = (CFGConditional) cfgNonConditional.autoChild;
                if (visited.get(child) == null || visited.get(child) == 1 || cfgNonConditional.parents.size() == 0) {
                    // we should put our code into the conditional;
                    if (child != null) {
                        child.lines.addAll(0, cfgNonConditional.lines);
                        if (cfgNonConditional.parents.size() == 0) {
                            return child;
                        }
                        for (CFGBlock parent : cfgNonConditional.parents) {
                            if (parent instanceof CFGConditional) {
                                if (cfgNonConditional == ((CFGConditional) parent).falseChild) {
                                    ((CFGConditional) parent).falseChild = child;
                                } else {
                                    ((CFGConditional) parent).trueChild = child;
                                }
                            } else {
                                ((CFGNonConditional) parent).autoChild = child;
                            }
                            child.parents.remove(cfgNonConditional);
                            child.parents.addAll(cfgNonConditional.parents);
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
            cfgConditional.trueChild.accept(this, symbolTable);
            cfgConditional.falseChild.accept(this, symbolTable);
            // Absorb the NOPs
            if (cfgConditional.trueChild instanceof NOP)
                cfgConditional.trueChild = ((NOP) cfgConditional.trueChild).autoChild;
            if (cfgConditional.falseChild instanceof NOP)
                cfgConditional.falseChild = ((NOP) cfgConditional.falseChild).autoChild;
//            return cfgConditional;
        }
        visited.put(cfgConditional, visited.get(cfgConditional) + 1);
        return cfgConditional;
    }

    @Override
    public CFGBlock visit(NOP nop, SymbolTable symbolTable) {
        visit((CFGNonConditional) nop, symbolTable);
        return nop.autoChild;
    }
}
