package decaf.common;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import decaf.codegen.codes.HasOperand;
import decaf.codegen.codes.StoreInstruction;
import decaf.dataflow.ssapasses.worklistitems.SsaEdge;
import decaf.cfg.BasicBlock;
import decaf.codegen.codes.Method;
import decaf.codegen.names.IrRegister;
import decaf.codegen.names.IrValue;
import decaf.dataflow.dominator.DominatorTree;

public class SSAEdgesUtil {
  private final Set<SsaEdge> ssaEdges;

  public SSAEdgesUtil(@NotNull Method method) {
    this.ssaEdges = computeSsaEdges(method);
  }

  private static Set<SsaEdge> computeSsaEdges(@NotNull Method method) {
    var dominatorTree = new DominatorTree(method.getEntryBlock());
    var ssaEdges = new HashSet<SsaEdge>();
    var lValueToDefMapping = new HashMap<IrRegister, StoreInstruction>();
    var basicBlocks = dominatorTree.preorder();

    for (BasicBlock basicBlock : basicBlocks) {
      basicBlock.getStoreInstructions()
                .forEach(storeInstruction -> {
                  if (storeInstruction.getDestination() instanceof IrRegister irRegister) {
                    lValueToDefMapping.put(
                        irRegister,
                        storeInstruction
                    );
                  }
                });
    }

    for (var basicBlock : basicBlocks) {
      for (var instruction : basicBlock.getInstructionList()) {
        if (instruction instanceof HasOperand hasOperand) {
          for (IrRegister irRegister : hasOperand.getOperandVirtualRegisters()) {
            if (lValueToDefMapping.containsKey(irRegister)) {
              ssaEdges.add(new SsaEdge(
                  lValueToDefMapping.get(irRegister),
                  hasOperand,
                  basicBlock
              ));
            } else if (!method.getParameterNames()
                              .contains(irRegister)) {
              throw new IllegalStateException(irRegister + " not found\n" + basicBlock.getInstructionList());
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

  public Set<HasOperand> getUses(@NotNull IrRegister irRegister) {
    return getSsaEdges().stream()
                        .filter(ssaEdge -> ssaEdge.getValue()
                                                  .equals(irRegister))
                        .map(SsaEdge::use)
                        .collect(Collectors.toUnmodifiableSet());
  }

  public void copyPropagate(
      @NotNull IrRegister toBeReplaced,
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
