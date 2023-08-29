package decaf.ir.dataflow.passes;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import decaf.ir.InstructionList;
import decaf.ir.cfg.BasicBlock;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.dataflow.analyses.AvailableExpressions;
import decaf.ir.dataflow.analyses.DataFlowAnalysis;
import decaf.ir.dataflow.dominator.DominatorTree;
import decaf.ir.dataflow.operand.Operand;
import decaf.ir.names.IrValue;

public class CommonSubExpressionEliminationPass extends OptimizationPass {
  boolean changeHappened = false;

  public CommonSubExpressionEliminationPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private static void discardKilledExpressions(
      StoreInstruction storeInstruction,
      HashMap<Operand, IrValue> expressionToVariable
  ) {
    // we iterate through a copy of the keys to prevent a ConcurrentCoModificationException
    for (Operand operand : new ArrayList<>(expressionToVariable.keySet())) {
      if (operand.contains(storeInstruction.getDestination())) {
        expressionToVariable.remove(operand);
      }
    }
  }

  /**
   * Maps each tac to its index in its corresponding tac list
   *
   * @param instructionList the subject tac list
   * @return a mapping from tac to its index
   */

  public static HashMap<Instruction, Integer> getTacToPosMapping(InstructionList instructionList) {
    var tacToPositionInList = new HashMap<Instruction, Integer>();
    var index = 0;
    for (Instruction tac : instructionList) {
      tacToPositionInList.put(
          tac,
          index
      );
      ++index;
    }
    return tacToPositionInList;
  }

  private static boolean isReassignedBeforeUse(
      BasicBlock basicBlock,
      Operand operand,
      StoreInstruction original
  ) {
    var indexOfOriginal = basicBlock.getInstructionList()
                                    .indexOf(original);
    assert indexOfOriginal != -1;
    if (indexOfOriginal == 0)
      return false;
    for (int indexOfCode = indexOfOriginal - 1; indexOfCode >= 0; indexOfCode--) {
      Instruction instruction = basicBlock.getInstructionList()
                                          .get(indexOfCode);
      if (instruction instanceof StoreInstruction storeInstruction) {
        if (operand.contains(storeInstruction.getDestination()) && !isTrivialAssignment(instruction)) {
          return true;
        }
      }
    }
    return false;
  }

  private static IrValue findExpressionAmongDominators(
      BasicBlock B,
      Operand operand,
      DominatorTree dominatorTree
  ) {
    for (BasicBlock dominator : dominatorTree.getDominators(B)) {
      for (Instruction instruction : dominator.getInstructionList()) {
        if (instruction instanceof BinaryInstruction || instruction instanceof UnaryInstruction) {
          final var storeInstruction = (StoreInstruction) instruction;
          if (operand.isContainedIn(storeInstruction)) {
            return storeInstruction.getDestination();
          }
        }
      }
    }
    // find first block which dominates B and doesn't
    throw new IllegalStateException("expression not found");
  }

  private void swapOut(
      InstructionList instructionList,
      Map<Instruction, Integer> instructionToIndexMapping,
      Instruction oldInstruction,
      Instruction newInstruction
  ) {
    final var indexOfOldInstruction = instructionToIndexMapping.get(oldInstruction);
    instructionList.replaceIfContainsInstructionAtIndex(
        indexOfOldInstruction,
        oldInstruction,
        newInstruction
    );
    instructionToIndexMapping.put(
        newInstruction,
        indexOfOldInstruction
    );
    changeHappened = true;
  }

  private void performCSE(
      StoreInstruction storeInstruction,
      Operand operand,
      InstructionList instructionList,
      HashMap<Operand, IrValue> expressionToVariable,
      HashMap<Instruction, Integer> instructionToPositionInInstructionList
  ) {

    if (expressionToVariable.containsKey(operand)) {
      // this computation has been made before, so swap it with the irAssignableValue that already stores the value
      var replacer = CopyInstruction.noMetaData(
          storeInstruction.getDestination(),
          expressionToVariable.get(operand)
      );
      var indexOfOldCode = instructionToPositionInInstructionList.get(storeInstruction);
            /* we check if the oldCode is indeed present in the tac list.
               if it is not, this method throws an IllegalArgumentException
            */
      instructionList.replaceIfContainsInstructionAtIndex(
          indexOfOldCode,
          storeInstruction,
          replacer
      );
      instructionToPositionInInstructionList.put(
          replacer,
          indexOfOldCode
      );
      changeHappened = true;
    }
    // the operand already doesn't contain any array name
    if (!operand.containsAny(globals())) {
      expressionToVariable.put(
          operand,
          storeInstruction.getDestination()
      );
    }
  }

    /*
    Search backward from the first occurrence to determine whether any of the operands of exp have been previously assigned to in the block.
    If so, this occurrence of `exp` is not a global common sub-expression;
    proceed to another expression or another block as appropriate.
    */

  public void performLocalCSE(BasicBlock basicBlock) {
    // this maps each expression to its irAssignableValue, for instance "a + b" -> c
    // while "a" and "b" have not been re-assigned, all occurrences of "a + b" are replaced with c
    final var expressionToVariable = new HashMap<Operand, IrValue>();
    // this maps each three address code to it's position in its corresponding TAC list
    // it is helpful when swapping out expressions with their replacements
    final var tacToPositionInList = getTacToPosMapping(basicBlock.getInstructionList());

    final var tacList = basicBlock.getInstructionList();

    for (StoreInstruction storeInstruction : basicBlock.getStoreInstructions()) {
      // we don't cache expressions involving array variables
      if (storeInstruction instanceof BinaryInstruction || storeInstruction instanceof UnaryInstruction) {
        storeInstruction.getOperandNoArray()
                        .ifPresent(
                            operand -> performCSE(
                                storeInstruction,
                                operand,
                                tacList,
                                expressionToVariable,
                                tacToPositionInList
                            ));
      }
      // remove all expressions which have been killed by this assignment
      discardKilledExpressions(
          storeInstruction,
          expressionToVariable
      );
    }
  }

  public void performGlobalCSE() {
    var availableExpressions = new AvailableExpressions(entryBlock);
    var dom = new DominatorTree(this.entryBlock);

    // we first perform local CSE for each basic block
    for (var basicBlock : getBasicBlocksList())
      performLocalCSE(basicBlock);

    // we then get the available expressions for each basic block in the CFG
    var availableExpressionsIn = availableExpressions.in;

    for (BasicBlock basicBlock : getBasicBlocksList()) {
      final var availableExpressionsForBlock = availableExpressionsIn.get(basicBlock);
      final var instructionToIndexMapping = DataFlowAnalysis.getInstructionToIndexMapping(basicBlock.getInstructionList());

      Objects.requireNonNull(
          availableExpressions,
          () -> "In[B] for basicBlock " + basicBlock + " not found"
      );

      for (Operand availableExpression : availableExpressionsForBlock) {
        // walk up a nodes dominators and replace this expression with the earliest instance of the store
        for (StoreInstruction storeInstruction : basicBlock.getStoreInstructions()) {
          if (storeInstruction instanceof UnaryInstruction || storeInstruction instanceof BinaryInstruction) {
            if (availableExpression.isContainedIn(storeInstruction) && !isReassignedBeforeUse(
                basicBlock,
                availableExpression,
                storeInstruction
            )) {
              final var expressionName = findExpressionAmongDominators(
                  basicBlock,
                  availableExpression,
                  dom
              );
              final var assign = CopyInstruction.noMetaData(
                  storeInstruction.getDestination(),
                  expressionName
              );
              swapOut(
                  basicBlock.getInstructionList(),
                  instructionToIndexMapping,
                  storeInstruction,
                  assign
              );
              break;
            }
            if (availableExpression.contains(storeInstruction.getDestination())) {
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public boolean runFunctionPass() {
    changeHappened = false;
    performGlobalCSE();
    return changeHappened;
  }
}