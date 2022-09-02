package edu.mit.compilers.dataflow.analyses;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.dataflow.Direction;
import edu.mit.compilers.utils.SetUtils;

/**
 * A definition d_x : x = e reaches a program point p if it appears without a redefinition on some path
 * from program entry to p
 */
public class ReachingDefinitions extends DataFlowAnalysis<StoreInstruction> {
    public ReachingDefinitions(BasicBlock basicBlock) {
        super(basicBlock);
    }

    private Set<StoreInstruction> gen(BasicBlock basicBlock) {
        var seenStoreVariables = new HashSet<>();
        var seenAgain = new HashSet<>();
        for (StoreInstruction storeInstruction : basicBlock.getStoreInstructions()) {
            if (seenStoreVariables.contains(storeInstruction.getStore())) {
                seenAgain.add(storeInstruction.getStore());
            }
            seenStoreVariables.add(storeInstruction.getStore());
        }
        return basicBlock.getStoreInstructions()
                         .stream()
                         .filter(store -> !seenAgain.contains(store.getStore()))
                         .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void computeUniversalSetsOfValues() {
        allValues = new HashSet<>();
        for (BasicBlock basicBlock : basicBlocks) {
            allValues.addAll(gen(basicBlock));
        }
    }

    @Override
    public Set<StoreInstruction> meet(BasicBlock basicBlock) {
        if (basicBlock.getPredecessors()
                      .isEmpty())
            return Collections.emptySet();

        var inSet = new HashSet<>(allValues);

        for (BasicBlock block : basicBlock.getPredecessors())
            inSet.retainAll(out(block));

        return inSet;
    }

    @Override
    public Set<StoreInstruction> transferFunction(StoreInstruction domainElement) {
        return null;
    }

    @Override
    public Direction direction() {
        return Direction.BACKWARDS;
    }

    @Override
    public void initializeWorkSets() {
        in = new HashMap<>();
        out = new HashMap<>();

        for (BasicBlock basicBlock : basicBlocks) {
            out.put(basicBlock, new HashSet<>());
            in.put(basicBlock, new HashSet<>());
        }

        out.put(entryBlock, gen(entryBlock));
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
            out.put(B, SetUtils.union(gen(B), SetUtils.difference(in(B), kill(B))));

            if (!out(B).equals(oldOut)) {
                workList.addAll(B.getSuccessors());
            }
        }
    }

    public Set<StoreInstruction> kill(BasicBlock basicBlock) {
        // we kill any definition
        var genSet = gen(basicBlock);
        var defined = basicBlock.getStoreInstructions()
                                .stream()
                                .map(StoreInstruction::getStore)
                                .collect(Collectors.toUnmodifiableSet());
        return allValues.stream()
                        .filter(store -> !genSet.contains(store) && defined.contains(store.getStore()))
                        .collect(Collectors.toUnmodifiableSet());
    }
}
