package decaf.dataflow.ssapasses;


import java.util.HashMap;

import decaf.cfg.BasicBlock;
import decaf.codegen.InstructionList;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.names.IrSsaRegister;
import decaf.dataflow.OptimizationContext;
import decaf.dataflow.dominator.DominatorTree;
import decaf.dataflow.operand.Operand;

public class CommonSubExpressionEliminationSsaPass extends SsaOptimizationPass {

  public CommonSubExpressionEliminationSsaPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  @Override
  protected void resetForPass() {

  }

  @Override
  public boolean runFunctionPass() {
    return performGlobalCSE();
  }


  private void performReplacement(
      StoreInstruction storeInstruction,
      InstructionList instructionList,
      IrSsaRegister expressionStoreLocation,
      Integer indexOfInstructionToBeReplaced
  ) {

    var replacer = CopyInstruction.noMetaData(
        storeInstruction.getDestination(),
        expressionStoreLocation
    );
    instructionList.replaceIfContainsInstructionAtIndex(
        indexOfInstructionToBeReplaced,
        storeInstruction,
        replacer
    );
  }


  public boolean performGlobalCSE() {
    var dom = new DominatorTree(getMethod().getEntryBlock());
    var expressionToBasicBlock = new HashMap<Operand, BasicBlock>();
    var expressionToIndexInBasicBlock = new HashMap<Operand, Integer>();
    var changesHappened = false;

    for (BasicBlock basicBlock : dom.preorder()) {
      var instructionList = basicBlock.getInstructionList();
      for (int indexOfInstruction = 0; indexOfInstruction < instructionList.size(); indexOfInstruction++) {
        Instruction instruction = instructionList.get(indexOfInstruction);
        if (instruction instanceof BinaryInstruction || instruction instanceof UnaryInstruction) {
          // check to see whether this expression has already been cached
          StoreInstruction storeInstruction = (StoreInstruction) instruction;
          var cachableExpression = storeInstruction.getOperand();
          if (expressionToBasicBlock.containsKey(cachableExpression)) {
            BasicBlock X = expressionToBasicBlock.get(cachableExpression);
            StoreInstruction Y = (StoreInstruction) X.getInstructionList()
                                                     .get(expressionToIndexInBasicBlock.get(cachableExpression));
            // do the replacement
            performReplacement(
                storeInstruction,
                instructionList,
                (IrSsaRegister) Y.getDestination(),
                indexOfInstruction
            );
            changesHappened = true;
          } else {
            expressionToBasicBlock.put(
                cachableExpression,
                basicBlock
            );
            expressionToIndexInBasicBlock.put(
                cachableExpression,
                indexOfInstruction
            );
          }
        }
      }
    }
    return changesHappened;
  }
}
