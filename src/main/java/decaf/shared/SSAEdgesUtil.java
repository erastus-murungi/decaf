package decaf.shared;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.dataflow.dominator.DominatorTree;
import decaf.ir.dataflow.ssapasses.worklistitems.SsaEdge;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrValue;

public class SSAEdgesUtil {
  private final Set<SsaEdge> ssaEdges;

  public SSAEdgesUtil(Method method) {
    this.ssaEdges = computeSsaEdges(method);
  }

  private static Set<SsaEdge> computeSsaEdges(Method method) {
    var dominatorTree = new DominatorTree(method.getEntryBlock());
    var ssaEdges = new HashSet<SsaEdge>();
    var lValueToDefMapping = new HashMap<IrSsaRegister, StoreInstruction>();
    var basicBlocks = dominatorTree.preorder();

    for (BasicBlock basicBlock : basicBlocks) {
      basicBlock.getStoreInstructions()
                .forEach(storeInstruction -> {
                  if (storeInstruction.getDestination() instanceof IrSsaRegister irSsaRegister) {
                    lValueToDefMapping.put(
                        irSsaRegister,
                        storeInstruction
                    );
                  }
                });
    }

    for (var basicBlock : basicBlocks) {
      for (var instruction : basicBlock.getInstructionList()) {
        if (instruction instanceof HasOperand hasOperand) {
          for (IrSsaRegister irSsaRegister : hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class)) {
            if (lValueToDefMapping.containsKey(irSsaRegister)) {
              ssaEdges.add(new SsaEdge(
                  lValueToDefMapping.get(irSsaRegister),
                  hasOperand,
                  basicBlock
              ));
            } else if (!method.getParameterNames()
                              .contains(irSsaRegister)) {
              throw new IllegalStateException(
                  instruction + "\n" + irSsaRegister + " not found\n" + basicBlock.getInstructionList());
            }
          }
        }
      }
    }
    return ssaEdges;
  }

  public Set<SsaEdge> getSsaEdges() {
    return ssaEdges;
  }

  public Set<HasOperand> getUses(IrSsaRegister irSsaRegister) {
    return getSsaEdges().stream()
                        .filter(ssaEdge -> ssaEdge.getValue()
                                                  .equals(irSsaRegister))
                        .map(SsaEdge::use)
                        .collect(Collectors.toUnmodifiableSet());
  }

  public void copyPropagate(
      IrSsaRegister toBeReplaced,
      IrValue replacer
  ) {
    var uses = getUses(toBeReplaced);
    var changesHappened = false;
    for (var use : uses) {
      changesHappened = changesHappened | use.replaceValue(
          toBeReplaced,
          replacer.copy()
      );
    }
  }
}
