package edu.mit.compilers.cfg;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Expression;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.UnconditionalBranch;
import edu.mit.compilers.ssa.Phi;

public class BasicBlock {

    @NotNull
    private final ArrayList<BasicBlock> predecessors = new ArrayList<>();
    @NotNull
    private final ArrayList<AST> astNodes = new ArrayList<>();
    @Nullable
    protected Expression branchCondition;
    @NotNull
    private BasicBlockType basicBlockType;
    @NotNull
    private InstructionList instructionList = new InstructionList();    // to be set by a visitor
    @Nullable
    private BasicBlock successor;
    @Nullable
    private BasicBlock alternateSuccessor;

    protected BasicBlock(@NotNull BasicBlockType basicBlockType, @Nullable Expression branchCondition) {
        this.branchCondition = branchCondition;
        this.basicBlockType = basicBlockType;
    }

    protected BasicBlock(@NotNull BasicBlockType basicBlockType) {
        this(basicBlockType, null);
    }

    public static BasicBlock noBranch() {
        return new BasicBlock(BasicBlockType.NO_BRANCH);
    }

    public static BasicBlock branch(@NotNull Expression branchCondition,
                                    @NotNull BasicBlock trueTarget,
                                    @NotNull BasicBlock falseTarget) {

        var basicBlock = new BasicBlock(BasicBlockType.BRANCH);
        basicBlock.branchCondition = branchCondition;
        basicBlock.setTrueTarget(trueTarget);
        basicBlock.alternateSuccessor = falseTarget;
        basicBlock.addAstNode(branchCondition);
        return basicBlock;
    }

    public boolean hasBranch() {
        return basicBlockType.equals(BasicBlockType.BRANCH);
    }

    public boolean hasNoBranch() {
        return basicBlockType.equals(BasicBlockType.NO_BRANCH) || basicBlockType.equals(BasicBlockType.NOP);
    }

    public boolean hasNoBranchNotNOP() {
        return basicBlockType.equals(BasicBlockType.NO_BRANCH);
    }

    public @NotNull ArrayList<AST> getAstNodes() {
        return astNodes;
    }

    public void addAstNode(AST astNode) {
        astNodes.add(astNode);
    }

    public void addAstNodes(Collection<AST> astNodes) {
        this.astNodes.addAll(astNodes);
    }

    public @NotNull BasicBlockType getBasicBlockType() {
        return basicBlockType;
    }

    public void setBasicBlockType(@NotNull BasicBlockType basicBlockType) {
        checkNotNull(basicBlockType);
        this.basicBlockType = basicBlockType;
    }

    public void removeAstNodes(Collection<AST> astNodes) {
        this.astNodes.removeAll(astNodes);
    }

    public @NotNull InstructionList getInstructionList() {
        return instructionList;
    }

    public void setInstructionList(@NotNull InstructionList instructionList) {
        this.instructionList = instructionList;
    }

    public List<Instruction> getInstructionListReversed() {
        var copyInstructionList = getCopyOfInstructionList();
        Collections.reverse(copyInstructionList);
        return copyInstructionList;
    }

    public String getLeader() {
        return astNodes.isEmpty() ? "None" : astNodes.get(0)
                .getSourceCode();
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

    public @NotNull ArrayList<BasicBlock> getPredecessors() {
        return predecessors;
    }

    @NotNull
    public Optional<Expression> getBranchCondition() {
        return Optional.ofNullable(branchCondition);
    }

    public List<BasicBlock> getSuccessors() {
        if (alternateSuccessor != null) {
            assert successor != null;
            return List.of(successor, alternateSuccessor);
        }
        assert successor != null;
        return List.of(successor);
    }

    public AST lastAstLine() {
        return astNodes.get(astNodes.size() - 1);
    }

    public String getLinesOfCodeString() {
        if (astNodes.isEmpty()) {
            if (!instructionList.isEmpty()) {
                return instructionList.get(0)
                        .syntaxHighlightedToString();
            }
        }
        return astNodes
                .stream()
                .map(AST::getSourceCode)
                .collect(Collectors.joining("\n"));
    }

    /**
     * get only TACs which change values of variables
     *
     * @return Iterator of Assignment TACs
     */
    public List<StoreInstruction> getStoreInstructions() {
        return instructionList.stream()
                .filter(tac -> tac instanceof StoreInstruction)
                .map(tac -> (StoreInstruction) tac)
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
        return instructionList.stream()
                .anyMatch(instruction -> instruction instanceof Phi);
    }

    public void addInstructionToTail(Instruction instruction) {
        if (!hasBranch()) {
            getInstructionList().add(instruction);
        } else {
            var instructionList = getInstructionList();
            int last = instructionList.size() - 1;
            var inst = instructionList.get(last);
            while (last >= 0 && (inst instanceof ConditionalBranch || inst instanceof UnconditionalBranch)) {
                inst = instructionList.get(last);
                last--;
            }
            instructionList.add(last, instruction);
        }

    }

    public BasicBlock getTrueTarget() {
        checkState(getBasicBlockType().equals(BasicBlockType.BRANCH), "can only request true target from a branching basic block");
        return successor;
    }

    public void setTrueTarget(@NotNull BasicBlock trueTarget) {
        this.successor = trueTarget;
    }

    public BasicBlock getFalseTarget() {
        checkState(getBasicBlockType().equals(BasicBlockType.BRANCH), "can only request false target from a branching basic block");
        return alternateSuccessor;
    }

    public void setFalseTarget(@NotNull BasicBlock falseTarget) {
        this.alternateSuccessor = falseTarget;
        getConditionalBranchInstruction().falseTarget = falseTarget;
    }

    public void setFalseTargetUnchecked(@NotNull BasicBlock falseTarget) {
        this.alternateSuccessor = falseTarget;
    }

    private ConditionalBranch getConditionalBranchInstruction() {
        return (ConditionalBranch) getInstructionList().stream().filter(instruction -> instruction instanceof ConditionalBranch).findFirst().orElseThrow(() -> new RuntimeException(toString()));
    }

    public @Nullable BasicBlock getSuccessor() {
        return successor;
    }

    public void setSuccessor(@Nullable BasicBlock successor) {
        this.successor = successor;
    }

    public void convertToBranchLess(@NotNull BasicBlock successor) {
        checkNotNull(successor);
        checkState(basicBlockType.equals(BasicBlockType.BRANCH), "basic block not branching");
        setBasicBlockType(BasicBlockType.NO_BRANCH);
        branchCondition = null;
        alternateSuccessor = null;
        setSuccessor(successor);
        checkState(getInstructionList().stream().filter(instruction -> instruction instanceof ConditionalBranch).count() == 1);
        getInstructionList().removeIf(instruction -> instruction instanceof ConditionalBranch);
        getSuccessors().stream()
                .filter(b -> !b.equals(successor))
                .forEach(node -> node.removePredecessor(this));
    }

    @Override
    public String toString() {
        return getLinesOfCodeString();
    }

    public BasicBlock getAlternateSuccessor() {
        return alternateSuccessor;
    }

    public enum BasicBlockType {
        NOP,
        BRANCH,
        NO_BRANCH,
    }
}
