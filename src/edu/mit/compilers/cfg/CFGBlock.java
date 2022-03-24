package edu.mit.compilers.cfg;


import edu.mit.compilers.ast.MethodDefinition;
import java.util.List;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;

public class CFGBlock {
    public ArrayList<CFGBlock> parents;
    public ArrayList<CFGLine> lines;

    public CFGBlock() {
        parents = new ArrayList<>();
        lines = new ArrayList<>();
    }

    public<T> T accept(CFGVisitor<T> visitor, SymbolTable symbolTable) {
        return visitor.visit(this, symbolTable);
    };
}
