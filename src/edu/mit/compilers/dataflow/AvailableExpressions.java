package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.codes.Triple;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AvailableExpressions extends DataFlowAnalysis<HasResult> {
    HashMap<HasResult, Integer> assignmentToId;
    HashMap<Integer, HasResult> idToAssignment;
    HashMap<HasResult, BasicBlock> assignmentToBasicBlock;

    public AvailableExpressions(BasicBlock entry) {
        List<BasicBlock> basicBlocks = DataFlowAnalysis.getReversePostOrder(entry);
        assignDefinitionsToIds(basicBlocks);
    }

    private void assignDefinitionsToIds(List<BasicBlock> basicBlocks) {
        assignmentToId = new HashMap<>();
        idToAssignment = new HashMap<>();
        assignmentToBasicBlock = new HashMap<>();
        int id = 0;
        for (BasicBlock basicBlock : basicBlocks) {
            for (Iterator<HasResult> it = basicBlock.assignmentIterator(); it.hasNext(); ) {
                final HasResult assignment = it.next();
                if (!assignmentToId.containsKey(assignment)) {
                    assignmentToId.putIfAbsent(assignment, id);
                    idToAssignment.putIfAbsent(id, assignment);
                    assignmentToBasicBlock.putIfAbsent(assignment, basicBlock);
                }
                id += 1;
            }
        }
        idToAssignment.forEach((k, v) -> System.out.format("%10s : %s\n", k, v));
        System.out.println(assignmentToId.entrySet());
    }

    private void computeBitSets(BasicBlock basicBlock) {

    }

    @Override
    public HasResult meet(Collection<HasResult> domainElements) {
        return null;
    }

    @Override
    public HasResult transferFunction(HasResult domainElement) {
        return null;
    }

    @Override
    public Direction direction() {
        return Direction.FORWARDS;
    }

    @Override
    public HasResult initializer() {
        return null;
    }

    @Override
    public Iterator<HasResult> order() {
        return null;
    }

    @Override
    public void runWorkList() {

    }

    public static boolean operatorIsCommutative(String operator) {
        return operator.equals(DecafScanner.PLUS) || operator.equals(DecafScanner.MULTIPLY);
    }

    /** Checks whether two assignment expressions are equivalent
     *
     * @param a An assignment such as a = b + c
     * @param b Another assignment such as b = c * d
     * @return true if the two assignments are equivalent and false otherwise
     *
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public static boolean expressionsAreIsomorphic(HasResult a, HasResult b) {
        if (a == null) {
            throw new IllegalArgumentException("first assignment is a null pointer");
        } else if (b == null) {
            throw new IllegalArgumentException("second assignment is a null pointer");
        }

        if (a instanceof Quadruple && b instanceof Quadruple) {
            final Quadruple aQuad = (Quadruple) a;
            final Quadruple bQuad = (Quadruple) b;
            if (aQuad.operator.equals(bQuad.operator)) {
                final String operator = aQuad.operator;
                if (operatorIsCommutative(operator)) {
                    return aQuad.fstOperand.equals(bQuad.fstOperand) && aQuad.sndOperand.equals(bQuad.sndOperand) ||
                            aQuad.sndOperand.equals(bQuad.fstOperand) && aQuad.fstOperand.equals(bQuad.sndOperand);
                } else {
                    return aQuad.fstOperand.equals(bQuad.fstOperand) && aQuad.sndOperand.equals(bQuad.sndOperand);
                }
            }
        } else if (a instanceof Triple && b instanceof Triple) {
            final Triple aTriple = (Triple) a;
            final Triple bTriple = (Triple) b;
            if (aTriple.operator.equals(bTriple.operator)) {
                return aTriple.operand.equals(bTriple.operand);
            }
        }
        return false;
    }
}
