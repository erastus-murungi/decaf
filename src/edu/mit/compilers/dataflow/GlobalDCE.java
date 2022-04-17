package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;

import java.util.stream.Collectors;

public class GlobalDCE {
    public static void performGlobalDeadCodeElimination(BasicBlock entryBlock) {
        var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);
        for (BasicBlock basicBlock : liveVariableAnalysis.basicBlocks) {

            final var tacList = basicBlock.codes();
            final var newTacList = basicBlock.threeAddressCodeList.clone();
            newTacList.getCodes().clear();

            final var inUseVars = liveVariableAnalysis.in.get(basicBlock).stream().map(useDef -> useDef.variable).collect(Collectors.toUnmodifiableSet());

            for (ThreeAddressCode tac : tacList) {
                if (tac instanceof HasResult) {
                    var hasResult = (HasResult) tac;
                    if (!inUseVars.contains(hasResult.getResultLocation())) {
                        continue;
                    }
                }
                if (tac instanceof HasOperand && ((HasOperand) tac).hasUnModifiedOperand()) {
                    var hasOperand = (HasOperand) tac;
                    var abstractName = hasOperand.getOperandNamesNoArray().get(0);
                    if (!inUseVars.contains(abstractName)) {
                        continue;
                    }
                }
                newTacList.addCode(tac);
            }
            basicBlock.threeAddressCodeList = newTacList;
        }
    }

}
