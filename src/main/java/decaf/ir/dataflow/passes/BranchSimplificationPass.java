package decaf.ir.dataflow.passes;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import decaf.ir.InstructionList;
import decaf.ir.cfg.BasicBlock;
import decaf.ir.codes.ConditionalBranch;
import decaf.ir.codes.Method;
import decaf.ir.dataflow.OptimizationContext;

public class BranchSimplificationPass extends OptimizationPass {
  boolean noChanges = true;

  public BranchSimplificationPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private boolean isTrue(ConditionalBranch conditionalBranch) {
    return conditionalBranch.getCondition()
                            .equals(InstructionSimplifyPass.mOne);
  }

  private boolean isFalse(ConditionalBranch conditionalBranch) {
    return conditionalBranch.getCondition()
                            .equals(InstructionSimplifyPass.mZero);
  }

  private Optional<ConditionalBranch> getConditional(InstructionList instructionList) {
    return instructionList
        .stream()
        .dropWhile(threeAddressCode -> !(threeAddressCode instanceof ConditionalBranch))
        .map(threeAddressCode -> (ConditionalBranch) threeAddressCode)
        .findFirst();
  }

  private Boolean getStateOfBlockCondition(BasicBlock basicBlockWithBranch) {
    Optional<ConditionalBranch> conditionalJump = getConditional(basicBlockWithBranch.getInstructionList());
    if (conditionalJump.isPresent()) {
      if (isTrue(conditionalJump.get())) {
        return true;
      } else if (isFalse(conditionalJump.get()))
        return false;
    }
    return null;
  }

  private void removeFalseBranch(BasicBlock basicBlockWithBranch) {
//        // if (true) || if (1)
//        // just reduce this to the true branch
//        var trueChild = basicBlockWithBranch.trueChild;
//        var falseChild = basicBlockWithBranch.falseChild;
//        basicBlockWithBranch.getInstructionList().reset(Collections.emptyList());
//        Label label = (Label) falseChild.getInstructionList().get(0);
//        if (label.aliasLabels.isEmpty()) {
//            var collect = falseChild.getInstructionList()
//                    .stream()
//                    .dropWhile(BranchSimplificationPass::isNotExitLabel)
//                    .collect(Collectors.toList());
//            trueChild.getInstructionList().addAll(collect);
//            falseChild.getInstructionList().reset(Collections.emptyList());
//        } else {
//            label.aliasLabels.remove(0);
//        }
//        noChanges = false;
  }

  private void removeTrueBranch(BasicBlock basicBlockWithBranch) {
//        // if (false) || if (0)
//        // just reduce this to the true branch
//        var trueChild = basicBlockWithBranch.trueChild;
//        basicBlockWithBranch.getInstructionList().reset(Collections.emptyList());
//        trueChild.getInstructionList().reset(Collections.emptyList());
//
//        var visited = new HashSet<InstructionList>();
//        visited.add(trueChild.getInstructionList());
//        var nextTacList = trueChild.getInstructionList().nextInstructionList;
//        var labelToUnconditionalJump = new HashMap<Label, InstructionList>();
//        while (nextTacList != null) {
//            if (visited.contains(nextTacList))
//                break;
//            visited.add(nextTacList);
//            Optional<ConditionalJump> conditional = getConditional(nextTacList);
//            if (nextTacList.size() == 1 && nextTacList.get(0) instanceof UnconditionalJump) {
//                Label label = ((UnconditionalJump) nextTacList.get(0)).goToLabel;
//                labelToUnconditionalJump.put(label, nextTacList);
//                nextTacList = nextBlockInPath.get(label);
//                if (label == null)
//                    throw new IllegalArgumentException();
//                continue;
//            }
//            if (conditional.isPresent() || !nextTacList.isEmpty() && nextTacList.get(0) instanceof Label) {
//                Label label = (Label) nextTacList.get(0);
//                if (labelToUnconditionalJump.containsKey(label)) {
//                    labelToUnconditionalJump.get(label)
//                            .reset(Collections.emptyList());
//                    break;
//                }
//                labelToUnconditionalJump.put(label, nextTacList);
//                if (label.aliasLabels.isEmpty()) {
//                    nextTacList.reset(Collections.emptyList());
//                } else {
//                    label.aliasLabels.remove(0);
//                    break;
//                }
//            }
//            else {
//                nextTacList.reset(Collections.emptyList());
//            }
//            nextTacList = nextTacList.nextInstructionList;
//        }
//        noChanges = false;
  }

  private void removeUselessBranches() {
    List<BasicBlock> evaluateToTrue = new ArrayList<>();
    List<BasicBlock> evaluateToFalse = new ArrayList<>();
    for (var basicBlock : getBasicBlocksList()) {
      if (basicBlock.hasBranch()) {
        Boolean evaluatesTrueBranch = getStateOfBlockCondition(basicBlock);
        if (evaluatesTrueBranch == null)
          continue;
        if (evaluatesTrueBranch)
          evaluateToTrue.add(basicBlock);
        else
          evaluateToFalse.add(basicBlock);
      }
    }
    if (evaluateToFalse.isEmpty() || evaluateToTrue.isEmpty())
      noChanges = true;
    evaluateToTrue.forEach(this::removeFalseBranch);
    evaluateToFalse.forEach(this::removeTrueBranch);
  }


  @Override
  public boolean runFunctionPass() {
    removeUselessBranches();
    return noChanges;
  }
}
