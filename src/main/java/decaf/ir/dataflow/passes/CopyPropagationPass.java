package decaf.ir.dataflow.passes;


import java.util.ArrayList;
import java.util.HashMap;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.codes.HasOperand;
import decaf.ir.codes.Instruction;
import decaf.ir.codes.Method;
import decaf.ir.codes.StoreInstruction;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.dataflow.analyses.AvailableCopies;
import decaf.ir.dataflow.operand.Operand;
import decaf.ir.dataflow.operand.UnmodifiedOperand;
import decaf.ir.names.IrMemoryAddress;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrValue;

public class CopyPropagationPass extends OptimizationPass {
  public CopyPropagationPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private static void propagateCopy(
      HasOperand hasOperand,
      HashMap<IrValue, IrValue> copies
  ) {
    if (copies.isEmpty())
      return;
    boolean notConverged = true;
    while (notConverged) {
      // we have to do this in a while loop because of how copy replacements propagate
      // for instance, lets imagine we have a = k
      // it possible that our copies map has y `replaces` k, and x `replaces` y and $0 `replaces` x
      // we want to eventually propagate so that a = $0
      notConverged = false;
      for (var toBeReplaced : hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class)) {
        if (hasOperand instanceof StoreInstruction && ((StoreInstruction) hasOperand).getDestination()
                                                                                     .equals(toBeReplaced))
          continue;
        if (copies.containsKey(toBeReplaced)) {
          var replacer = copies.get(toBeReplaced);
          if (!isTrivialAssignment(hasOperand)) {
            hasOperand.replaceValue(
                toBeReplaced,
                replacer
            );
          } else {
            continue;
          }
          // it is not enough to set `converged` to true if we have found our replacer
          // we do this extra check, i.e the check : "!replacerName.equals(abstractName)"
          // because our copies map sometimes contains entries like "x `replaces` x"
          // without this check, we sometimes enter an infinite loop
          if (!replacer.equals(toBeReplaced)) {
            notConverged = true;
          }
        }
      }
    }
  }

  private boolean performLocalCopyPropagation(
      BasicBlock basicBlock,
      HashMap<IrValue, IrValue> copies
  ) {
    // we need a reference tac list to know whether any changes occurred
    final var tacList = basicBlock.getCopyOfInstructionList();
    final var newTacList = basicBlock.getInstructionList();

    newTacList.clear();

    for (Instruction instruction : tacList) {
      // we only perform copy propagation on instructions with variables
      if (instruction instanceof HasOperand) {
        propagateCopy(
            (HasOperand) instruction,
            copies
        );
      }

      if (instruction instanceof StoreInstruction hasResult) {
        var resultLocation = hasResult.getDestination();
        if (!(resultLocation instanceof IrMemoryAddress)) {
          for (var assignableName : new ArrayList<>(copies.keySet())) {
            var replacer = copies.get(assignableName);
            if (assignableName.equals(resultLocation) ||
                replacer.equals(resultLocation)) {
              // delete all assignments that are invalidated by this current instruction
              // if it is an assignment
              copies.remove(assignableName);
            }
          }
          // insert this new pair into copies
          var operand = hasResult.getOperandNoArrayNoGlobals(globals());
          if (operand.isPresent()) {
            Operand op = operand.get();
            if (op instanceof UnmodifiedOperand) {
              IrValue name = ((UnmodifiedOperand) operand.get()).irValue;
              copies.put(
                  resultLocation,
                  name
              );
            }
          }
        }
      }
      newTacList.add(instruction);
    }
    return newTacList.equals(tacList);
  }

  public void performGlobalCopyPropagation() {
    var availableCopiesIn = new AvailableCopies(entryBlock).availableCopies;

    // remove all copy instructions which involve global variables
    availableCopiesIn.forEach(((basicBlock, assigns) -> new ArrayList<>(assigns
                                                                            .keySet())
        .forEach(resultLocation -> {
          if (globals().contains(resultLocation)) {
            assigns.remove(resultLocation);
          }
        })));

    // perform copy propagation until no more changes are observed
    var notConverged = true;
    while (notConverged) {
      notConverged = false;
      for (BasicBlock basicBlock : getBasicBlocksList()) {
        if (!performLocalCopyPropagation(
            basicBlock,
            availableCopiesIn.get(basicBlock)
        )) {
          notConverged = true;
        }
      }
    }
  }

  @Override
  public boolean runFunctionPass() {
    final var oldCodes = entryBlock.getCopyOfInstructionList();
    performGlobalCopyPropagation();
    return !oldCodes.equals(entryBlock.getInstructionList());
  }
}
