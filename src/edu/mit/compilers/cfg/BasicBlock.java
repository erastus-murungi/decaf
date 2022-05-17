package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BasicBlock {
    private final ArrayList<BasicBlock> predecessors;
    // to be set by a visitor
    public InstructionList instructionList;

    public ArrayList<AST> lines;

    private Label label;

    public void setLabel(Label label) {
        if (label == null) {
            throw new IllegalStateException();
        }
        this.label = label;
    }

    public Label getLabel() {
        return label;
    }

    public String getLeader() {
        return lines.isEmpty() ? "None" : lines.get(0).getSourceCode();
    }

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
        instructionList = new InstructionList();
        setLabel(new Label(TemporaryNameIndexGenerator.getNextLabel()));
    }

    public abstract <T> T accept(BasicBlockVisitor<T> visitor, SymbolTable symbolTable);

    public String getLinesOfCodeString() {
        if (lines.isEmpty()) {
            if (!instructionList.isEmpty()) {
                return instructionList.get(0).repr();
            }
        }
        return lines
                .stream()
                .map(AST::getSourceCode)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String toString() {
        return getLinesOfCodeString();
    }

    /**
     * get only TACs which change values of variables
     * @return Iterator of Assignment TACs
     */
    public List<Store> getStores() {
        return instructionList.stream()
                .filter(tac -> tac instanceof Store)
                .map(tac -> (Store) tac)
                .collect(Collectors.toList());
    }

    /**
     * note that this method returns an {@link ArrayList} not a {@link InstructionList}
     *
     * @return an {@link ArrayList} of the Instructions in this {@link InstructionList}
     */
    public List<Instruction> getCopyOfInstructionList() {
        return new ArrayList<>(instructionList);
    }
}
