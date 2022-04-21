package edu.mit.compilers.dataflow.passes;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.StringConstantName;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.dataflow.operand.AugmentedOperand;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class DeadCodeEliminationPass extends OptimizationPass {
    public DeadCodeEliminationPass(Set<AbstractName> globalVariables, BasicBlock entryBlock) {
        super(globalVariables, entryBlock);
    }

    // return whether an instruction of the form x = x
    private boolean isTrivialAssignment(ThreeAddressCode threeAddressCode) {
        if (threeAddressCode instanceof Assign) {
            var assign = (Assign) threeAddressCode;
            if (assign.assignmentOperator.equals(DecafScanner.ASSIGN)) {
                return assign.dst.equals(assign.operand);
            }
        }
        return false;
    }

    private boolean augmentedAssignVariablesNeeded(Set<AbstractName> neededSet, ThreeAddressCode tac) {
        if (tac instanceof HasOperand) {
            var hasOperand = (HasOperand) tac;
            var operand = hasOperand.getOperand();
            if (operand instanceof AugmentedOperand) {
                return hasOperand.getOperandNamesNoArray()
                        .stream()
                        .allMatch(this::allConstants);
            }
        }
        return false;

    }

    private boolean allConstants(AbstractName abstractName) {
        return abstractName instanceof ConstantName || abstractName instanceof StringConstantName;
    }

    private boolean resultVariableIsGlobal(ThreeAddressCode threeAddressCode) {
        if (threeAddressCode instanceof HasResult)
            return globalVariables.contains(((HasResult) threeAddressCode).dst);
        return false;
    }

    private boolean allVariablesInNeededSet(Set<AbstractName> neededSet, ThreeAddressCode tac) {
        var allOperandsAreConstants = tac.getNames()
                .stream()
                .allMatch(this::allConstants);
        return resultVariableIsGlobal(tac) || augmentedAssignVariablesNeeded(neededSet, tac) || allOperandsAreConstants || neededSet.containsAll(tac.getNamesNoArrayNoGlobals(globalVariables));
    }

    public void performGlobalDeadCodeElimination() {
        final var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);

        for (var basicBlock : basicBlocks) {

            // the set of variables actually needed later in the program
            final var neededSet = liveVariableAnalysis
                    .in
                    .get(basicBlock)
                    .stream()
                    .map(useDef -> useDef.variable)
                    .collect(Collectors.toSet());

            // if all the variables used in this instruction are not in the needed set, then ignore
            // this instruction while reconstructing this basicBlocks TAC list
            // note that we ignore global variables and array names here
            // remove useless assignments like x = x
            basicBlock.threeAddressCodeList.reset(basicBlock.codes()
                    .stream()
                    .filter(tac -> allVariablesInNeededSet(neededSet, tac) && !isTrivialAssignment(tac))
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.codes();
        performGlobalDeadCodeElimination();
        return oldCodes.equals(entryBlock.threeAddressCodeList.getCodes());
    }
}
