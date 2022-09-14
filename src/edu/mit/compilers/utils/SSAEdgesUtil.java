package edu.mit.compilers.utils;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.dataflow.dominator.DominatorTree;
import edu.mit.compilers.dataflow.ssapasses.worklistitems.SsaEdge;

public class SSAEdgesUtil {
    private final Set<SsaEdge> ssaEdges;

    public SSAEdgesUtil(@NotNull Method method) {
        this.ssaEdges = computeSsaEdges(method);
    }

    private static Set<SsaEdge> computeSsaEdges(@NotNull Method method) {
        var dominatorTree = new DominatorTree(method.entryBlock);
        var ssaEdges = new HashSet<SsaEdge>();
        var lValueToDefMapping = new HashMap<IrRegister, StoreInstruction>();
        var basicBlocks = dominatorTree.preorder();

        for (BasicBlock basicBlock : basicBlocks) {
            basicBlock.getStoreInstructions()
                    .forEach(
                            storeInstruction -> {
                                if (storeInstruction.getDestination() instanceof IrRegister irRegister) {
                                    lValueToDefMapping.put(irRegister, storeInstruction);
                                }
                            }
                    );
        }

        for (BasicBlock basicBlock : basicBlocks) {
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof HasOperand hasOperand) {
                    for (IrRegister irRegister : hasOperand.getOperandVirtualRegisters()) {
                        if (lValueToDefMapping.containsKey(irRegister)) {
                            ssaEdges.add(new SsaEdge(lValueToDefMapping.get(irRegister), hasOperand, basicBlock));
                        } else if (method.getParameterNames().contains(irRegister)) {

                        } else {
                            throw new IllegalStateException(irRegister + " not found" + basicBlock.getInstructionList());
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

    public void copyPropagate(@NotNull IrRegister toBeReplaced, IrValue replacer) {
        var uses = getUses(toBeReplaced);
        var changesHappened = false;
        for (var use : uses) {
            changesHappened = changesHappened | use.replaceValue(toBeReplaced, replacer.copy());
        }
    }
}
