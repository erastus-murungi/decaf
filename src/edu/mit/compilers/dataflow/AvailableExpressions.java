package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Assignment;
import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.codes.Triple;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AvailableExpressions extends DataFlowAnalysis<Assignment> {
    HashMap<Assignment, Integer> assignmentToId;
    HashMap<Integer, Assignment> idToAssignment;
    HashMap<Assignment, BasicBlock> assignmentToBasicBlock;

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
            for (Iterator<Assignment> it = basicBlock.assignmentIterator(); it.hasNext(); ) {
                final Assignment assignment = it.next();
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
    public Assignment meet(Collection<Assignment> domainElements) {
        return null;
    }

    @Override
    public Assignment transferFunction(Assignment domainElement) {
        return null;
    }

    @Override
    public Direction direction() {
        return Direction.FORWARDS;
    }

    @Override
    public Assignment initializer() {
        return null;
    }

    @Override
    public Iterator<Assignment> order() {
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
    public static boolean expressionsAreIsomorphic(Assignment a, Assignment b) {
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
