package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;
import edu.mit.compilers.codegen.codes.AllocateInstruction;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.ssa.Phi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BasicBlock {
    private Label label;
    private final ArrayList<BasicBlock> predecessors;
    // to be set by a visitor
    private InstructionList instructionList;

    private final ArrayList<AST> astNodes;

    public ArrayList<AST> getAstNodes() {
        return astNodes;
    }

    public void addAstNode(AST astNode) {
        astNodes.add(astNode);
    }

    public void addAstNodes(Collection<AST> astNodes) {
        this.astNodes.addAll(astNodes);
    }

    public void removeAstNodes(Collection<AST> astNodes) {
        this.astNodes.removeAll(astNodes);
    }

    public InstructionList getInstructionList() {
        return instructionList;
    }

    public void setInstructionList(InstructionList instructionList) {
        this.instructionList = instructionList;
    }

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
        return astNodes.isEmpty() ? "None" : astNodes.get(0).getSourceCode();
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

    public void clearPredecessors() {
        predecessors.clear();
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
        return astNodes.get(astNodes.size() - 1);
    }

    public BasicBlock() {
        predecessors = new ArrayList<>();
        astNodes = new ArrayList<>();
        instructionList = new InstructionList();
        setLabel(new Label(TemporaryNameIndexGenerator.getNextLabel()));
    }

    public abstract <T> T accept(BasicBlockVisitor<T> visitor);

    public String getLinesOfCodeString() {
        if (astNodes.isEmpty()) {
            if (!instructionList.isEmpty()) {
                return instructionList.get(0).syntaxHighlightedToString();
            }
        }
        return astNodes
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
    public List<StoreInstruction> getStores() {
        return instructionList.stream()
                .filter(tac -> tac instanceof StoreInstruction)
                .map(tac -> (StoreInstruction) tac)
                .collect(Collectors.toList());
    }

    public List<AllocateInstruction> getAllocations() {
        return instructionList.stream()
                              .filter(tac -> tac instanceof AllocateInstruction)
                              .map(tac -> (AllocateInstruction) tac)
                              .collect(Collectors.toList());
    }

    public List<Phi> getPhiFunctions() {
        return instructionList.stream()
                              .filter(tac -> tac instanceof Phi)
                              .map(tac -> (Phi) tac)
                              .collect(Collectors.toList());
    }

    public List<Instruction> getNonPhiInstructions() {
        return instructionList.stream()
                              .filter(tac -> !(tac instanceof Phi))
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

    public boolean phiPresent() {
        return instructionList.stream().anyMatch(instruction -> instruction instanceof Phi);
    }

    public void addInstructionToTail(Instruction instruction) {
        if (this instanceof BasicBlockBranchLess) {
            getInstructionList().add(instruction);
        } else {
            var instructionList = getInstructionList();
            int last = instructionList.size() - 1;
            var inst = instructionList.get(last);
            while (last >= 0 && (inst instanceof ConditionalBranch || inst instanceof UnconditionalJump)) {
                inst = instructionList.get(last);
                last --;
            }
            instructionList.add(last, instruction);
        }

    }
}
