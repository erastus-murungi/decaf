package decaf.dataflow.analyses;

import com.google.common.collect.Sets;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import decaf.cfg.BasicBlock;
import decaf.codegen.codes.StoreInstruction;
import decaf.dataflow.Direction;

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
      if (seenStoreVariables.contains(storeInstruction.getDestination())) {
        seenAgain.add(storeInstruction.getDestination());
      }
      seenStoreVariables.add(storeInstruction.getDestination());
    }
    return basicBlock.getStoreInstructions()
                     .stream()
                     .filter(store -> !seenAgain.contains(store.getDestination()))
                     .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public void computeUniversalSetsOfValues() {
    allTS = new HashSet<>();
    for (BasicBlock basicBlock : basicBlocks) {
      allTS.addAll(gen(basicBlock));
    }
  }

  @Override
  public Set<StoreInstruction> meet(BasicBlock basicBlock) {
    if (basicBlock.getPredecessors()
                  .isEmpty())
      return Collections.emptySet();

    var inSet = new HashSet<>(allTS);

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
      out.put(
          basicBlock,
          new HashSet<>()
      );
      in.put(
          basicBlock,
          new HashSet<>()
      );
    }

    out.put(
        entryBlock,
        gen(entryBlock)
    );
  }

  @Override
  public void runWorkList() {
    var workList = new ArrayDeque<>(basicBlocks);
    workList.remove(entryBlock);

    while (!workList.isEmpty()) {
      final BasicBlock B = workList.pop();
      var oldOut = out(B);


      // IN[B] = intersect OUT[p] for all p in predecessors
      in.put(
          B,
          meet(B)
      );

      // OUT[B] = gen[B] ∪ IN[B] - KILL[B]
      out.put(
          B,
          Sets.union(
              gen(B),
              Sets.difference(
                  in(B),
                  kill(B)
              )
          )
      );

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
                            .map(StoreInstruction::getDestination)
                            .collect(Collectors.toUnmodifiableSet());
    return allTS.stream()
                .filter(store -> !genSet.contains(store) && defined.contains(store.getDestination()))
                .collect(Collectors.toUnmodifiableSet());
  }
}
