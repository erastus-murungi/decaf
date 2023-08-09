package decaf.dataflow.ssapasses;


import java.util.HashSet;
import java.util.Set;

import decaf.cfg.BasicBlock;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.FunctionCallWithResult;
import decaf.codegen.codes.HasOperand;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.names.IrSsaRegister;
import decaf.codegen.names.IrValue;
import decaf.dataflow.OptimizationContext;

public class DeadStoreEliminationSsaPass extends SsaOptimizationPass {
  public DeadStoreEliminationSsaPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private static boolean storeInstructionIsDead(
      StoreInstruction storeInstruction,
      Set<IrValue> usedIrValues
  ) {
    if (!(storeInstruction.getDestination() instanceof IrSsaRegister)) {
      return false;
    }
    if (storeInstruction instanceof CopyInstruction || storeInstruction instanceof UnaryInstruction ||
        storeInstruction instanceof BinaryInstruction || storeInstruction instanceof FunctionCallWithResult) {
      return !usedIrValues.contains(storeInstruction.getDestination());
    }
    return false;
  }

  @Override
  protected void resetForPass() {
  }

  @Override
  public boolean runFunctionPass() {
    // look at the whole program and find all the uses
    var changesHappened = false;
    var used = new HashSet<IrValue>();

    for (BasicBlock basicBlock : getBasicBlocksList()) {
      for (Instruction instruction : basicBlock.getInstructionList()) {
        if (instruction instanceof HasOperand hasOperand) {
          used.addAll(hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class));
        }
      }
    }
    for (BasicBlock basicBlock : getBasicBlocksList()) {
      var deadStores = basicBlock.getStoreInstructions()
                                 .stream()
                                 .filter(storeInstruction -> storeInstructionIsDead(
                                     storeInstruction,
                                     used
                                 ))
                                 .toList();
      basicBlock.getInstructionList()
                .removeAll(deadStores);
    }
    return changesHappened;
  }
}
