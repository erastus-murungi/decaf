package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Array;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class BasicBlock {
    private final ArrayList<BasicBlock> predecessors;
    // to be set by the threeAddressCodeList
    public ThreeAddressCodeList threeAddressCodeList;

    public ArrayList<AST> lines;

    public void addPredecessor(BasicBlock predecessor) {
        predecessors.add(predecessor);
    }

    public void addPredecessors(Collection<BasicBlock> predecessors) {
        this.predecessors.addAll(predecessors);
    }

    public void removePredecessor(BasicBlock predecessor) {
        predecessors.remove(predecessor);
    }

    public boolean isRoot() {
        return predecessors.isEmpty();
    }

    public boolean doesNotContainPredecessor(BasicBlock predecessor) {
        return !predecessors.contains(predecessor);
    }

    public ArrayList<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public abstract List<BasicBlock> getSuccessors();

    public AST lastASTLine() {
        return lines.get(lines.size() - 1);
    }

    public BasicBlock() {
        predecessors = new ArrayList<>();
        lines = new ArrayList<>();
        threeAddressCodeList = ThreeAddressCodeList.empty();
    }

    public abstract <T> T accept(BasicBlockVisitor<T> visitor, SymbolTable symbolTable);

    public String getLabel() {
        return lines
                .stream()
                .map(AST::getSourceCode)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String toString() {
        return getLabel();
    }

    /**
     * get only TACs which change values of variables
     * @return Iterator of Assignment TACS
     */
    public List<HasResult> assignments() {
        return StreamSupport
                .stream(threeAddressCodeList.spliterator(), false)
                .filter(tac -> tac instanceof HasResult)
                .map(tac -> (HasResult) tac)
                .collect(Collectors.toList());
    }

    public List<ThreeAddressCode> codes() {
        return new ArrayList<>(threeAddressCodeList.getCodes());
    }
}
