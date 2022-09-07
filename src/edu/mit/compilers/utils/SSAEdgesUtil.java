package edu.mit.compilers.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.dominator.DominatorTree;
import edu.mit.compilers.dataflow.ssapasses.worklistitems.SsaEdge;

public class SSAEdgesUtil {
    private final Set<SsaEdge> ssaEdges;

    public SSAEdgesUtil(Method method) {
        this.ssaEdges = computeSsaEdges(method);
    }

    private static Set<SsaEdge> computeSsaEdges(Method method) {
        var dominatorTree = new DominatorTree(method.entryBlock);
        var ssaEdges = new HashSet<SsaEdge>();
        var lValueToDefMapping = new HashMap<LValue, StoreInstruction>();
        var basicBlocks = dominatorTree.preorder();

        for (BasicBlock basicBlock : basicBlocks) {
            basicBlock.getStoreInstructions()
                    .forEach(
                            storeInstruction -> lValueToDefMapping.put(storeInstruction.getDestination(), storeInstruction)
                    );
        }

        for (BasicBlock basicBlock : basicBlocks) {
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof HasOperand hasOperand) {
                    for (LValue lValue : hasOperand.getOperandLValues()) {
                        if (lValueToDefMapping.containsKey(lValue)) {
                            ssaEdges.add(new SsaEdge(lValueToDefMapping.get(lValue), hasOperand, basicBlock));
                        } else if (method.getParameterNames().contains(lValue)) {

                        } else {
                            throw new IllegalStateException(lValue + " not found" + basicBlock.getInstructionList());
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

    public Set<HasOperand> getUses(LValue lValue) {
        return getSsaEdges().stream()
                .filter(ssaEdge -> ssaEdge.getValue()
                        .equals(lValue))
                .map(SsaEdge::use)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void copyPropagate(LValue toBeReplaced, Value replacer) {
        var uses = getUses(toBeReplaced);
        var changesHappened = false;
        for (var use : uses) {
            changesHappened = changesHappened | use.replaceValue(toBeReplaced, replacer.copy());
        }
    }
}
