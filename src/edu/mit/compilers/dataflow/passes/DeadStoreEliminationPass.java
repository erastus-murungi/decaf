package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.MethodCallNoResult;
import edu.mit.compilers.codegen.codes.MethodCallSetResult;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.StringConstantName;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;

public class DeadStoreEliminationPass extends OptimizationPass {
    public DeadStoreEliminationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }


    private boolean isConstant(AbstractName abstractName) {
        return abstractName instanceof ConstantName || abstractName instanceof StringConstantName;
    }

    private boolean allVariableConstants(ThreeAddressCode tac) {
        if (tac instanceof HasResult)
            return ((HasOperand) tac).getOperandNames().stream().allMatch(this::isConstant);
        return false;
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
                        if (globalVariables.contains(store) || allVariableConstants(tac)) {
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

            var forwardTacList = new ArrayList<>(newTacList.getCodes());
            Collections.reverse(forwardTacList);
            newTacList.getCodes().clear();

            int startIndex = -1;
            for (var tac: forwardTacList) {
                ++startIndex;
                if (tac instanceof HasResult) {
                    // this is a store
                    // check if it is in uses
                    // if it is not used afterwards, remove it
                    var store = ((HasResult) tac).getResultLocation();
                    if (tac instanceof MethodCallSetResult) {
                        newTacList.addCode(tac);
                        continue;
                    }

                    if (!(store instanceof ArrayName)) {
                        if (globalVariables.contains(store)) {
                            newTacList.addCode(tac);
                            continue;
                        }
                        if (!isUsedBeforeNextStore(store, forwardTacList, startIndex) && !(isLastStore(store, forwardTacList, startIndex) && usedLaterGlobal.contains(store)))
                            continue;
                    }
                }
                newTacList.addCode(tac);
            }
        }
    }

    private boolean isUsedBeforeNextStore(AbstractName store, List<ThreeAddressCode> threeAddressCodeList, int startIndex) {
        HashSet<AbstractName> used = new HashSet<>();

        for (var i = startIndex + 1; i < threeAddressCodeList.size(); i++) {
            var tac = threeAddressCodeList.get(i);
            if (tac instanceof HasResult)
                if (((HasResult) tac).getResultLocation().equals(store)) {
                    // add the operands
                    // we add this extra check because of stores like a = a + b
                    if (tac instanceof HasOperand) {
                        used.addAll(((HasOperand) tac).getOperandNames());
                    }
                    break;
                }
            if (tac instanceof HasOperand) {
                used.addAll(tac.getNames());
            }
        }
        return used.contains(store);
    }

    private boolean isLastStore(AbstractName store, List<ThreeAddressCode> threeAddressCodeList, int startIndex) {
        for (var i = startIndex + 1; i < threeAddressCodeList.size(); i++) {
            var tac = threeAddressCodeList.get(i);
            if (tac instanceof HasResult)
                if (((HasResult) tac).getResultLocation().equals(store)) {
                    return false;
                }
        }
        return true;
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.codes();
        performDeadStoreElimination();
        return oldCodes.equals(entryBlock.threeAddressCodeList.getCodes());
    }
}
