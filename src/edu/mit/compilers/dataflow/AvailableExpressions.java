package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.codes.MethodCallSetResult;
import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.codes.Triple;
import edu.mit.compilers.codegen.names.TemporaryName;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.dataflow.computation.BinaryComputation;
import edu.mit.compilers.dataflow.computation.Computation;
import edu.mit.compilers.dataflow.computation.UnaryComputation;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AvailableExpressions extends DataFlowAnalysis<Computation> {
    // the set of all assignments in the program
    HashSet<Computation> allComputations;
    // the list of all basic blocks
    List<BasicBlock> basicBlocks;

    HashMap<BasicBlock, HashSet<Computation>> out;
    HashMap<BasicBlock, HashSet<Computation>> in;
    HashMap<BasicBlock, HashSet<Computation>> gen;
    HashMap<BasicBlock, HashSet<Computation>> kill;

    NOP entryBlock;

    public String getResultForPrint() {
        return Stream
                .of(basicBlocks)
                .flatMap(Collection::stream)
                .map(basicBlock -> in.get(basicBlock))
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Computation::getIndex))
                .map(Computation::toString)
                .collect(Collectors.joining("\n"));
    }

    public AvailableExpressions(BasicBlock entry) {
        attachEntryNode(entry);
        findAllBasicBlocksInReversePostOrder();
        findUniversalSetOfAssignments();
        initializeWorkSets();
        runWorkList();
    }

    private void findAllBasicBlocksInReversePostOrder() {
        basicBlocks = DataFlowAnalysis.getReversePostOrder(entryBlock);
    }

    private void attachEntryNode(BasicBlock basicBlock) {
        entryBlock = new NOP("Entry");
        entryBlock.autoChild = basicBlock;
        basicBlock.addPredecessor(entryBlock);
    }

    private void findUniversalSetOfAssignments() {
        allComputations = new HashSet<>();
        for (BasicBlock basicBlock : basicBlocks) {
            allComputations.addAll(gen(basicBlock));
        }
    }

    private void initializeWorkSets() {
        out = new HashMap<>();
        in = new HashMap<>();
        gen = new HashMap<>();
        kill = new HashMap<>();

        for (BasicBlock basicBlock : basicBlocks) {
            out.put(basicBlock, allComputations); // all expressions are available at initialization time
            in.put(basicBlock, new HashSet<>());
            gen.put(basicBlock, new HashSet<>());
            kill.put(basicBlock, new HashSet<>());
        }

        out.put(entryBlock, new HashSet<>());
    }

    public Computation meet(Collection<Computation> domainElements) {
        return null;
    }

    @Override
    public Computation transferFunction(Computation domainElement) {
        return null;
    }

    @Override
    public Direction direction() {
        return Direction.FORWARDS;
    }

    @Override
    public Computation initializer() {
        return null;
    }

    @Override
    public Iterator<Computation> order() {
        return null;
    }

    public HashSet<Computation> intersection(Collection<BasicBlock> blocks) {
        if (blocks.isEmpty()) {
            return new HashSet<>();
        }
        HashSet<Computation> in = new HashSet<>(allComputations);
        for (BasicBlock block : blocks) {
            in.retainAll(out.get(block));
        }
        return in;
    }

    public HashSet<Computation> difference(HashSet<Computation> first, HashSet<Computation> second) {
        var firstCopy = new HashSet<>(first);
        firstCopy.removeAll(second);
        return firstCopy;
    }

    public HashSet<Computation> union(HashSet<Computation> first, HashSet<Computation> second) {
        var firstCopy = new HashSet<>(first);
        firstCopy.addAll(second);
        return firstCopy;
    }

    @Override
    public void runWorkList() {
        var workList = new ArrayList<>(basicBlocks);

        workList.remove(entryBlock);

        // IN[entry] = ∅
        in.put(entryBlock, new HashSet<>());

        out.put(entryBlock, gen(entryBlock));

        while (!workList.isEmpty()) {
            final BasicBlock B = workList.remove(0);
            var oldOut = out.get(B);

            var inSet = intersection(B.getPredecessors());
            var killSet = kill(B, new HashSet<>(allComputations));

            // IN[B] = intersect OUT[p] for all p in predecessors
            in.put(B, inSet);

            // OUT[B] = gen[B] ∪ IN[B] - KILL[B]
            out.put(B, union(gen(B), difference(inSet, killSet)));

            if (!out
                    .get(B)
                    .equals(oldOut)) {
                workList.addAll(B.getSuccessors());
            }
        }
    }


    private HashSet<Computation> kill(BasicBlock basicBlock, HashSet<Computation> superSet) {
        HashSet<Computation> killedExpressions = new HashSet<>();

        for (HasResult assignment: basicBlock.assignments()) {
            // add all the computations that contain operands
            // that get re-assigned by the current stmt to
            // killedExpressions
            if (assignment instanceof Triple) {
                Triple triple = (Triple) assignment;
                if (operatorAssigns(triple.operator)) {
                    for (Computation comp : superSet) {
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

    private HashSet<Computation> gen(BasicBlock basicBlock) {
        var validComputations = new HashSet<Computation>();
        for (HasResult assignment: basicBlock.assignments()) {
            // add this unary computation to the list of valid expressions
            if (assignment instanceof Triple) {
                Triple triple = (Triple) assignment;
                if (triple.operand instanceof VariableName || triple.operand instanceof TemporaryName) {
                    validComputations.add(new UnaryComputation(triple));
                }
            }
            // add this binary computation to the list of valid expressions
            else if (assignment instanceof Quadruple) {
                Quadruple quadruple = (Quadruple) assignment;
                if (quadruple.fstOperand instanceof VariableName && quadruple.sndOperand instanceof VariableName) {
                    validComputations.add(new BinaryComputation(quadruple));
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
                    for (Computation computation : new HashSet<>(validComputations)) {
                        if (computation.contains(assignment.getResultLocation())) {
                            validComputations.remove(computation);
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
