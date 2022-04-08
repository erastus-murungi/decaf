package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CFGBlock {
    public ArrayList<CFGBlock> parents;
    public ArrayList<CFGLine> lines;

    public abstract List<CFGBlock> getSuccessors();

    public AST lastASTLine() {
        return lines.get(lines.size() - 1).ast;
    }


    public CFGBlock() {
        parents = new ArrayList<>();
        lines = new ArrayList<>();
    }

    public abstract <T> T  accept(CFGVisitor<T> visitor, SymbolTable symbolTable);

    public String getLabel() {
        return lines.stream().map(cfgLine -> cfgLine.ast.getSourceCode()).collect(Collectors.joining("\n"));
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
