package decaf.ir.cfg;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Expression;
import decaf.ir.IndexManager;
import decaf.ir.InstructionList;
import decaf.ir.codes.ConditionalBranch;
import decaf.ir.codes.Instruction;
import decaf.ir.codes.StoreInstruction;
import decaf.ir.codes.UnconditionalBranch;
import decaf.ir.codes.WithTarget;
import decaf.ir.ssa.Phi;

public class BasicBlock {
  @NotNull
  private final ArrayList<BasicBlock> predecessors = new ArrayList<>();
  @NotNull
  private final ArrayList<AST> astNodes = new ArrayList<>();
  @NotNull
  private final Set<WithTarget> tributaries = new HashSet<>();
  @Nullable
  protected Expression branchCondition;
  @NotNull
  private BasicBlockType basicBlockType;
  @Nullable
  private InstructionList instructionList = null;
  @Nullable
  private BasicBlock successor;
  @Nullable
  private BasicBlock alternateSuccessor;


  protected BasicBlock(
      @NotNull BasicBlockType basicBlockType,
      @Nullable Expression branchCondition
  ) {
    this.branchCondition = branchCondition;
    this.basicBlockType = basicBlockType;
  }

  protected BasicBlock(BasicBlockType basicBlockType) {
    this(
        basicBlockType,
        null
    );
  }

  public static BasicBlock noBranch() {
    return new BasicBlock(BasicBlockType.NO_BRANCH);
  }

  public static BasicBlock branch(
      @NotNull Expression branchCondition,
      @NotNull BasicBlock trueTarget,
      @NotNull BasicBlock falseTarget
  ) {

    var basicBlock = new BasicBlock(BasicBlockType.BRANCH);
    basicBlock.branchCondition = branchCondition;
    basicBlock.setTrueTarget(trueTarget);
    basicBlock.alternateSuccessor = falseTarget;
    basicBlock.addAstNode(branchCondition);
    return basicBlock;
  }

  public static void correctTributaries(
      BasicBlock basicBlock,
      BasicBlock replacer
  ) {
    basicBlock.getTributaries()
              .forEach(withTarget -> withTarget.replaceTarget(replacer));
  }

  public List<WithTarget> getWithTargets() {
    assert instructionList != null;
    return instructionList.stream()
                          .filter(instruction -> instruction instanceof WithTarget)
                          .map(instruction -> (WithTarget) instruction)
                          .toList();
  }

  public Set<WithTarget> getTributaries() {
    return Set.copyOf(tributaries);
  }

  public void addTributary(WithTarget withTarget) {
    tributaries.add(withTarget);
  }

  public void removeTributary(WithTarget withTarget) {
    tributaries.remove(withTarget);
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

  public void addAstNodes(Collection<? extends AST> astNodes) {
    this.astNodes.addAll(astNodes);
  }

  public @NotNull BasicBlockType getBasicBlockType() {
    return basicBlockType;
  }

  public void setBasicBlockType(BasicBlockType basicBlockType) {
    checkNotNull(basicBlockType);
    this.basicBlockType = basicBlockType;
  }

  public void removeAstNodes(Collection<AST> astNodes) {
    this.astNodes.removeAll(astNodes);
  }

  public @NotNull InstructionList getInstructionList() {
    if (instructionList == null)
      throw new IllegalStateException("instruction list not initialized");
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
    return astNodes.isEmpty() ? "None": astNodes.get(0)
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

  public Optional<Expression> getBranchCondition() {
    return Optional.ofNullable(branchCondition);
  }

  public List<BasicBlock> getSuccessors() {
    List<BasicBlock> successors = new ArrayList<>();
    if (successor != null) successors.add(successor);
    if (alternateSuccessor != null && alternateSuccessor != successor) successors.add(alternateSuccessor);
    return List.copyOf(successors);
  }

  public AST lastAstLine() {
    return astNodes.get(astNodes.size() - 1);
  }

  public String getLinesOfCodeString() {
    if (astNodes.isEmpty()) {
      if (!getInstructionList().isEmpty()) {
        return getInstructionList().get(0)
                              .syntaxHighlightedToString();
      }
    }
    return astNodes.stream()
                   .map(AST::getSourceCode)
                   .collect(Collectors.joining("\n"));
  }

  /**
   * get only TACs which change values of variables
   *
   * @return Iterator of Assignment TACs
   */
  public List<StoreInstruction> getStoreInstructions() {
    return getInstructionList().stream()
                          .filter(tac -> tac instanceof StoreInstruction)
                          .map(tac -> (StoreInstruction) tac)
                          .collect(Collectors.toList());
  }

  public List<Phi> getPhiFunctions() {
    return getInstructionList().stream()
                          .filter(tac -> tac instanceof Phi)
                          .map(tac -> (Phi) tac)
                          .collect(Collectors.toList());
  }

  public List<Instruction> getNonPhiInstructions() {
    return getInstructionList().stream()
                          .filter(tac -> !(tac instanceof Phi))
                          .collect(Collectors.toList());
  }

  /**
   * note that this method returns an {@link ArrayList} not a {@link InstructionList}
   *
   * @return an {@link ArrayList} of the Instructions in this {@link InstructionList}
   */
  public List<Instruction> getCopyOfInstructionList() {
    return new ArrayList<>(getInstructionList());
  }

  public boolean phiPresent() {
    return getInstructionList().stream()
                          .anyMatch(instruction -> instruction instanceof Phi);
  }

  public void addInstructionToTail(Instruction instruction) {
    if (!hasBranch()) {
      getInstructionList().add(instruction);
    } else {
      var instructionList = getInstructionList();
      int indexOfLastInstruction = instructionList.size() - 1;
      while (indexOfLastInstruction >= 0 && (instructionList.get(indexOfLastInstruction) instanceof ConditionalBranch ||
          instructionList.get(indexOfLastInstruction) instanceof UnconditionalBranch)) {
        indexOfLastInstruction--;
      }
      instructionList.add(
          indexOfLastInstruction + 1,
          instruction
      );
    }

  }

  public BasicBlock getTrueTarget() {
    checkState(
        getBasicBlockType().equals(BasicBlockType.BRANCH),
        "can only request true target from a branching basic block"
    );
    return successor;
  }

  public void setTrueTarget(BasicBlock trueTarget) {
    this.successor = trueTarget;
  }

  public BasicBlock getFalseTarget() {
    checkState(
        getBasicBlockType().equals(BasicBlockType.BRANCH),
        "can only request false target from a branching basic block"
    );
    return alternateSuccessor;
  }

  public void setFalseTarget(BasicBlock falseTarget) {
    this.alternateSuccessor = falseTarget;
    getConditionalBranchInstruction().setFalseTarget(falseTarget);
  }

  public void setFalseTargetUnchecked(BasicBlock falseTarget) {
    this.alternateSuccessor = falseTarget;
  }

  public ConditionalBranch getConditionalBranchInstruction() {
    return (ConditionalBranch) getInstructionList().stream()
                                                   .filter(instruction -> instruction instanceof ConditionalBranch)
                                                   .findFirst()
                                                   .orElseThrow(() -> new RuntimeException(toString()));
  }

  public @Nullable BasicBlock getSuccessor() {
    return successor;
  }

  public void setSuccessor(@Nullable BasicBlock successor) {
    this.successor = successor;
  }

  private List<BasicBlock> genRemoved(BasicBlock newSuccessor) {
    var removed = new ArrayList<BasicBlock>();
    if (successor != newSuccessor) removed.add(successor);
    if (alternateSuccessor != newSuccessor) removed.add(alternateSuccessor);
    return removed;
  }

  private void fixPhiNodes(BasicBlock newSuccessor) {
    var removed = genRemoved(newSuccessor);
  }

  public void convertToBranchLess(BasicBlock newSuccessor) {
    checkNotNull(newSuccessor);
    checkState(
        basicBlockType.equals(BasicBlockType.BRANCH),
        "basic block not branching"
    );
    setBasicBlockType(BasicBlockType.NO_BRANCH);
    fixPhiNodes(newSuccessor);
    branchCondition = null;
    alternateSuccessor = null;
    setSuccessor(newSuccessor);
    checkState(getInstructionList().stream()
                                   .filter(instruction -> instruction instanceof ConditionalBranch)
                                   .count() == 1);
    getInstructionList().removeIf(instruction -> instruction instanceof ConditionalBranch);
    getSuccessors().stream()
                   .filter(b -> !b.equals(newSuccessor))
                   .forEach(node -> node.removePredecessor(this));

  }

  public void convertToBranchLessSkipTrue() {
    checkState(
        basicBlockType.equals(BasicBlockType.BRANCH),
        "basic block not branching"
    );
    setBasicBlockType(BasicBlockType.NO_BRANCH);
    branchCondition = null;
    successor = getConditionalBranchInstruction().getTarget();
    alternateSuccessor = null;

    checkState(getInstructionList().stream()
                                   .filter(instruction -> instruction instanceof ConditionalBranch)
                                   .count() == 1);
    getInstructionList().removeIf(instruction -> instruction instanceof ConditionalBranch);
    getSuccessors().stream()
                   .filter(b -> !b.equals(successor))
                   .forEach(node -> node.removePredecessor(this));
  }

  @Override
  public String toString() {
    return getLinesOfCodeString();
  }

  public @Nullable BasicBlock getAlternateSuccessor() {
    return alternateSuccessor;
  }

  public BasicBlock split(int index) {
    checkArgument(index >= 0);
    checkArgument(index < getInstructionList().size());

    var newBasicBlock = BasicBlock.noBranch();
    newBasicBlock.successor = this;
    newBasicBlock.getPredecessors()
                 .addAll(this.predecessors);
    this.predecessors.clear();
    this.addPredecessor(newBasicBlock);
    newBasicBlock.getInstructionList()
                 .addAll(getInstructionList().subList(
                     0,
                     index
                 ));
    this.getInstructionList()
        .removeAll(newBasicBlock.getInstructionList());
    newBasicBlock.getInstructionList()
                 .forEach(
                     instruction -> {
                       if (instruction.getSource() != null) {
                         newBasicBlock.addAstNode(instruction.getSource());
                       }
                     }
                 );
    correctTributaries(
        this,
        newBasicBlock
    );
    newBasicBlock.tributaries.addAll(this.getTributaries());

    this.getTributaries()
        .clear();
    newBasicBlock.getInstructionList()
                 .setLabel(this.getInstructionList()
                               .getLabel());
    this.getInstructionList()
        .setLabel(IndexManager.genLabelIndex());
    return newBasicBlock;
  }

  public enum BasicBlockType {
    NOP, BRANCH, NO_BRANCH,
  }
}
