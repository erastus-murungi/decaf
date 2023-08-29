package decaf.ir.dataflow.ssapasses;


import java.util.HashSet;
import java.util.Set;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrValue;

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
