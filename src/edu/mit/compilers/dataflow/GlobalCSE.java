package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;

import java.util.*;

public class GlobalCSE {
    private static void swapOut(ThreeAddressCodeList tacList,
                                Map<ThreeAddressCode, Integer> tacToPositionInList,
                                ThreeAddressCode oldCode,
                                ThreeAddressCode newCode) {
        final var indexOfOldCode = tacToPositionInList.get(oldCode);
        tacList.replaceIfContainsOldCodeAtIndex(indexOfOldCode, oldCode, newCode);
        tacToPositionInList.put(newCode, indexOfOldCode);
    }


    public static void performGlobalCSE(BasicBlock root, Set<AbstractName> globalVariables) {
        var basicBlocks = DataFlowAnalysis.getReversePostOrder(root);
        var availableExpressions = new AvailableExpressions(root);

        // we first perform local CSE for each basic block
        for (BasicBlock basicBlock : basicBlocks)
            LocalCSE.performLocalCSE(basicBlock, globalVariables);

        // we then get the available expressions for each basic block in the CFG
        var availableExpressionsIn = availableExpressions.in;

        for (BasicBlock basicBlock : basicBlocks) {
            final var availableExpressionsForBlock = availableExpressionsIn.get(basicBlock);
            final var tacList = basicBlock.threeAddressCodeList;
            final var tacToPositionInList = DataFlowAnalysis.getTacToPosMapping(tacList);

            Objects.requireNonNull(availableExpressions, () -> "In[B] for basicBlock " + basicBlock + " not found");

            for (Operand computation : availableExpressionsForBlock) {
                for (HasResult hasResult : basicBlock.assignments()) {
                    if (hasResult instanceof Triple || hasResult instanceof Quadruple) {
                        if (computation.isContainedIn(hasResult)) {
                            final var operand = backPropagateToEliminateCommonExpressionFromCFG(basicBlock, computation);
                            final var assign = Assign.ofRegularAssign(hasResult.getResultLocation(), operand);
                            swapOut(tacList, tacToPositionInList, hasResult, assign);
                            break;
                        }
                        if (computation.contains(hasResult.getResultLocation())) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private static AbstractName backPropagateToEliminateCommonExpressionFromCFG(BasicBlock basicBlock,
                                                                                Operand operand) {
        AssignableName uniqueName = null;
        final var queue = new ArrayDeque<>(basicBlock.getPredecessors());
        final var visited = new HashSet<>();

        visited.add(basicBlock);
        while (!queue.isEmpty()) {
            final var current = queue.pop();
            if (visited.contains(current))
                continue;
            visited.add(current);

            var computationFound = false;
            final var updateTacList = new ArrayList<ThreeAddressCode>();

            final var tacListReversed = current.codes();
            Collections.reverse(tacListReversed);
            for (ThreeAddressCode threeAddressCode : tacListReversed) {
                if (!computationFound) {
                    if (threeAddressCode instanceof Triple || threeAddressCode instanceof Quadruple) {
                        final var hasResult = (HasResult) threeAddressCode;
                        if (operand.isContainedIn(hasResult)) {
                            uniqueName = hasResult.getResultLocation();
                            computationFound = true;
                        }
                    }
                }
                updateTacList.add(threeAddressCode);
            }
            Collections.reverse(updateTacList);
            final var codes = current.threeAddressCodeList.getCodes();
            codes.clear();
            codes.addAll(updateTacList);

            if (!computationFound) {
                queue.addAll(current.getPredecessors());
            }
        }
        Objects.requireNonNull(uniqueName, "uniqueName is null");
        return uniqueName;
    }
}