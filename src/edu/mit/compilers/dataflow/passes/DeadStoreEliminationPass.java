package edu.mit.compilers.dataflow.passes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;

public class DeadStoreEliminationPass extends OptimizationPass {
    public DeadStoreEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    private void performDeadStoreElimination() {
        final var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);

        for (var basicBlock : basicBlocks) {
            var tacList = basicBlock.codes();
            Collections.reverse(tacList);
            final var newTacList = basicBlock.threeAddressCodeList;
            newTacList.getCodes().clear();

            final var usedBeforeGlobal = Collections.unmodifiableSet(liveVariableAnalysis.neededVariables(basicBlock));
            final var usedLaterGlobal = Collections.unmodifiableSet(liveVariableAnalysis.usedLater(basicBlock));
            final var usedInBlock = new HashSet<AbstractName>();
            // we iterate in reverse
            for (var tac: tacList) {
                if (isTrivialAssignment(tac))
                    continue;
                if (tac instanceof HasResult) {
                    // this is a store
                    // check if it is in uses
                    // if it is not used afterwards, remove it
                    var store = ((HasResult) tac).getResultLocation();

                    if (!(store instanceof ArrayName)) {
                        if (globalVariables.contains(store)) {
                            usedInBlock.addAll(tac.getNames());
                            newTacList.addCode(tac);
                            continue;
                        }

                        // if this store is not used
                        if (!usedLaterGlobal.contains(store) && !usedInBlock.contains(store) && (methodBegin.isMain() || (!globalVariables.contains(store))))
                            continue;
                        if (!usedBeforeGlobal.contains(store) && (methodBegin.isMain() || (!globalVariables.contains(store))))
                            continue;
                    }
                }
                if (tac instanceof HasOperand)
                    usedInBlock.addAll(tac.getNames());

                newTacList.addCode(tac);
            }
            Collections.reverse(newTacList.getCodes());
        }
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.codes();
        performDeadStoreElimination();
        return oldCodes.equals(entryBlock.threeAddressCodeList.getCodes());
    }
}
