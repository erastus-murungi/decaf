package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CFGBlock {
    private final ArrayList<CFGBlock> predecessors;
    public ArrayList<CFGLine> lines;

    public void addPredecessor(CFGBlock predecessor) {
        predecessors.add(predecessor);
    }

    public void addPredecessors(Collection<CFGBlock> predecessors) {
        this.predecessors.addAll(predecessors);
    }

    public void removePredecessor(CFGBlock predecessor) {
        predecessors.remove(predecessor);
    }

    public boolean isRoot() {
        return predecessors.size() == 0;
    }

    public boolean hasPredecessor(CFGBlock predecessor) {
        return predecessors.contains(predecessor);
    }

    public ArrayList<CFGBlock> getPredecessors() {
        return predecessors;
    }

    public abstract List<CFGBlock> getSuccessors();

    public AST lastASTLine() {
        return lines.get(lines.size() - 1).ast;
    }


    public CFGBlock() {
        predecessors = new ArrayList<>();
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
