package edu.mit.compilers.dataflow.analyses;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.Direction;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.copy.CopyQuadruple;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.*;

public class AvailableCopies extends DataFlowAnalysis<CopyQuadruple> {
    // each basicBlock maps to another map of (u -> v) pairs
    public HashMap<BasicBlock, HashMap<AbstractName, Operand>> availableCopies;

    @Override
    public void computeUniversalSetsOfValues() {
        allValues = new HashSet<>();
        for (BasicBlock basicBlock : basicBlocks)
            allValues.addAll(copy(basicBlock));
    }

    public AvailableCopies(BasicBlock basicBlock) {
        super(basicBlock);
        populateAvailableCopies();
    }

    private void populateAvailableCopies() {
        availableCopies = new HashMap<>();
        for (BasicBlock basicBlock : basicBlocks) {
            if (basicBlock.equals(entryBlock)) continue;
            var copiesForBasicBlock = new HashMap<AbstractName, Operand>();
            for (CopyQuadruple copyQuadruple : in(basicBlock)) {
                copiesForBasicBlock.put(copyQuadruple.u, copyQuadruple.v);
            }
            availableCopies.put(basicBlock, copiesForBasicBlock);
        }
    }

    @Override
    public Set<CopyQuadruple> transferFunction(CopyQuadruple domainElement) {
        return null;
    }

    @Override
    public Direction direction() {
        return Direction.FORWARDS;
    }

    @Override
    public void initializeWorkSets() {
        out = new HashMap<>();
        in = new HashMap<>();

        for (BasicBlock basicBlock : basicBlocks) {
            out.put(basicBlock, new HashSet<>()); // all copies are available at initialization time
            in.put(basicBlock, new HashSet<>(allValues));
        }
        // IN[entry] = ∅
        in.put(entryBlock, new HashSet<>());
        out.put(entryBlock, copy(entryBlock));
    }

    @Override
    public void runWorkList() {
        var workList = new ArrayDeque<>(basicBlocks);
        workList.remove(entryBlock);

        while (!workList.isEmpty()) {
            final var B = workList.pop();
            var oldOut = out(B);

            // IN[B] = intersect OUT[p] for all p in predecessors
            in.put(B, meet(B));

            // OUT[B] = gen[B] ∪ IN[B] - KILL[B]
            out.put(B, union(copy(B), difference(in(B), kill(B))));

            if (!out(B).equals(oldOut)) {
                workList.addAll(B.getSuccessors());
            }
        }
    }

    @Override
    public Set<CopyQuadruple> meet(BasicBlock basicBlock) {
        if (basicBlock.getPredecessors().isEmpty())
            return Collections.emptySet();

        var inSet = in(basicBlock);

        for (BasicBlock block : basicBlock.getPredecessors())
            inSet.retainAll(out(block));
        return inSet;
    }

    private HashSet<CopyQuadruple> copy(BasicBlock basicBlock) {
        var copyQuadruples = new HashSet<CopyQuadruple>();

        for (HasResult hasResult : basicBlock.assignments()) {
            // check to see whether any existing assignments are invalidated by this one
            if (!(hasResult.getResultLocation() instanceof ArrayName)) {
                final AssignableName resulLocation = hasResult.getResultLocation();
                // remove any copy's where u, or v are being reassigned by "resultLocation"
                copyQuadruples.removeIf(copyQuadruple -> copyQuadruple.contains(resulLocation));
            }
            if (hasResult instanceof Assign) {
                Assign assign = (Assign) hasResult;
                if ((!assign.assignmentOperator.equals(DecafScanner.ASSIGN)) || assign.operand instanceof ArrayName)
                    continue;
            }
            var computation = hasResult.getComputationNoArray();
            computation.ifPresent(value -> copyQuadruples.add(new CopyQuadruple(hasResult.getResultLocation(), value,
                    basicBlock
                            .codes()
                            .indexOf(hasResult), basicBlock)));

        }
        return copyQuadruples;
    }


    private HashSet<CopyQuadruple> kill(BasicBlock basicBlock) {
        var superSet = new HashSet<>(allValues);
        var killedCopyQuadruples = new HashSet<CopyQuadruple>();
        for (HasResult hasResult : basicBlock.assignments()) {
            if (!(hasResult.getResultLocation() instanceof ArrayName)) {
                final AssignableName resulLocation = hasResult.getResultLocation();
                for (CopyQuadruple copyQuadruple : superSet) {
                    if (copyQuadruple.basicBlock != basicBlock && copyQuadruple.contains(resulLocation)) {
                        killedCopyQuadruples.add(copyQuadruple);
                    }
                }
            }
        }
        return killedCopyQuadruples;
    }
}
