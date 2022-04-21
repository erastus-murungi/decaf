package edu.mit.compilers.dataflow.analyses;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.MethodCallSetResult;
import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.codes.Triple;
import edu.mit.compilers.codegen.names.TemporaryName;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.dataflow.Direction;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.operand.BinaryOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnaryOperand;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.*;

public class AvailableExpressions extends DataFlowAnalysis<Operand> {
    public AvailableExpressions(BasicBlock entry) {
        super(entry);
    }

    @Override
    public void computeUniversalSetsOfValues() {
        allValues = new HashSet<>();
        for (BasicBlock basicBlock : basicBlocks)
            allValues.addAll(gen(basicBlock));
    }

    @Override
    public void initializeWorkSets() {
        out = new HashMap<>();
        in = new HashMap<>();

        for (BasicBlock basicBlock : basicBlocks) {
            out.put(basicBlock, allValues); // all expressions are available at initialization time
            in.put(basicBlock, new HashSet<>());
        }

        // IN[entry] = ∅
        in.put(entryBlock, new HashSet<>());
        out.put(entryBlock, gen(entryBlock));
    }


    @Override
    public Set<Operand> transferFunction(Operand domainElement) {
        return null;
    }

    @Override
    public Direction direction() {
        return Direction.FORWARDS;
    }

    @Override
    public void runWorkList() {
        var workList = new ArrayDeque<>(basicBlocks);
        workList.remove(entryBlock);

        while (!workList.isEmpty()) {
            final BasicBlock B = workList.pop();
            var oldOut = out(B);


            // IN[B] = intersect OUT[p] for all p in predecessors
            in.put(B, meet(B));

            // OUT[B] = gen[B] ∪ IN[B] - KILL[B]
            out.put(B, union(gen(B), difference(in(B), kill(B))));

            if (!out(B).equals(oldOut)) {
                workList.addAll(B.getSuccessors());
            }
        }
    }

    @Override
    public Set<Operand> meet(BasicBlock basicBlock) {
        if (basicBlock.getPredecessors().isEmpty())
            return Collections.emptySet();

        var inSet = new HashSet<>(allValues);

        for (BasicBlock block : basicBlock.getPredecessors())
            inSet.retainAll(out.get(block));
        return inSet;
    }


    private HashSet<Operand> kill(BasicBlock basicBlock) {
        var superSet = new ArrayList<>(allValues);
        var killedExpressions = new HashSet<Operand>();

        for (HasResult assignment: basicBlock.assignments()) {
            // add all the computations that contain operands
            // that get re-assigned by the current stmt to
            // killedExpressions
            if (assignment instanceof Triple) {
                Triple triple = (Triple) assignment;
                if (operatorAssigns(triple.operator)) {
                    for (Operand comp : superSet) {
                        if (comp.contains(triple.getResultLocation())) {
                            killedExpressions.add(comp);
                        }
                    }
                }
            }
        }
        return killedExpressions;
    }

    private boolean operatorAssigns(String operator) {
        return operator.contains(DecafScanner.ASSIGN);
    }

    private HashSet<Operand> gen(BasicBlock basicBlock) {
        var validComputations = new HashSet<Operand>();
        for (HasResult assignment: basicBlock.assignments()) {
            // add this unary computation to the list of valid expressions
            if (assignment instanceof Triple) {
                Triple triple = (Triple) assignment;
                if (triple.operand instanceof VariableName || triple.operand instanceof TemporaryName) {
                    validComputations.add(new UnaryOperand(triple));
                }
            }
            // add this binary computation to the list of valid expressions
            else if (assignment instanceof Quadruple) {
                Quadruple quadruple = (Quadruple) assignment;
                if (quadruple.fstOperand instanceof VariableName && quadruple.sndOperand instanceof VariableName) {
                    validComputations.add(new BinaryOperand(quadruple));
                }
            }

            // remove all expressions invalidated by this assignment
            if (assignment instanceof Triple || assignment instanceof MethodCallSetResult) {
                String operator;
                if (assignment instanceof Triple) {
                    operator = ((Triple) assignment).operator;
                } else {
                    operator = "=";
                }
                if (operatorAssigns(operator)) {
                    for (Operand operand : new HashSet<>(validComputations)) {
                        if (operand.contains(assignment.getResultLocation())) {
                            validComputations.remove(operand);
                        }
                    }
                }
            }
        }
        return validComputations;
    }

    public static boolean operatorIsCommutative(String operator) {
        return operator.equals(DecafScanner.PLUS) || operator.equals(DecafScanner.MULTIPLY);
    }

    /**
     * Checks whether two assignment expressions are equivalent
     *
     * @param a An assignment such as a = b + c
     * @param b Another assignment such as b = c * d
     * @return true if the two assignments are equivalent and false otherwise
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
