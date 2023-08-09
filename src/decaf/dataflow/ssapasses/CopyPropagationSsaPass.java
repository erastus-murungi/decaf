package decaf.dataflow.ssapasses;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import decaf.cfg.BasicBlock;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.HasOperand;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.names.IrSsaRegister;
import decaf.codegen.names.IrValue;
import decaf.dataflow.OptimizationContext;
import decaf.dataflow.dominator.DominatorTree;
import decaf.ssa.SSA;

public class CopyPropagationSsaPass extends SsaOptimizationPass {
  List<SSACopyOptResult> resultList = new ArrayList<>();

  public CopyPropagationSsaPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private boolean performGlobalCopyPropagation() {
    var changesHappened = false;
    var dom = new DominatorTree(getMethod().getEntryBlock());
    // maps (toBeReplaced -> replacer)
    var copiesMap = new HashMap<IrSsaRegister, IrValue>();

    for (BasicBlock basicBlock : getBasicBlocksList()) {
      for (StoreInstruction storeInstruction : basicBlock.getStoreInstructions()) {
        if (storeInstruction instanceof CopyInstruction copyInstruction) {
          var replacer = copyInstruction.getValue();
          var toBeReplaced = copyInstruction.getDestination();
          if (toBeReplaced instanceof IrSsaRegister irSsaRegisterToBeReplaced) {
            checkState(
                !copiesMap.containsKey(irSsaRegisterToBeReplaced),
                "CopyPropagation : Invalid SSA form: " + toBeReplaced + " found twice in program"
            );
            copiesMap.put(
                irSsaRegisterToBeReplaced,
                replacer
            );
            // as a quick optimization, we could delete this storeInstruction here
          }
        }
      }
    }

    for (BasicBlock basicBlock : dom.preorder()) {
      for (Instruction instruction : basicBlock.getNonPhiInstructions()) {
        if (instruction instanceof HasOperand hasOperand) {
          for (IrSsaRegister toBeReplaced : hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class)) {
            if (copiesMap.containsKey(toBeReplaced)) {
              var before = hasOperand.copy();
              var replacer = copiesMap.get(toBeReplaced);
              // we have to do this in a while loop because of how copy replacements propagate
              // for instance, lets imagine we have a = k
              // it possible that our copies map has y `replaces` k, and x `replaces` y and $0 `replaces` x
              // we want to eventually propagate so that a = $0
              while (replacer instanceof IrSsaRegister && copiesMap.containsKey((IrSsaRegister) replacer)) {
                replacer = copiesMap.get(replacer);
              }
              hasOperand.replaceValue(
                  toBeReplaced,
                  replacer
              );
              resultList.add(new SSACopyOptResult(
                  before,
                  instruction,
                  toBeReplaced,
                  replacer
              ));
              changesHappened = true;
            }
          }
        }
      }
    }
    return changesHappened;
  }

  @Override
  protected void resetForPass() {
    resultList = new ArrayList<>();
  }

  @Override
  public boolean runFunctionPass() {
    resetForPass();
    var changesHappened = performGlobalCopyPropagation();
    SSA.verifySsa(method);
    return changesHappened;
  }
}
