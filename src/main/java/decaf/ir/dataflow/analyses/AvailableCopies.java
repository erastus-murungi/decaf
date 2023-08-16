package decaf.ir.dataflow.analyses;

import com.google.common.collect.Sets;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.codes.StoreInstruction;
import decaf.ir.names.IrMemoryAddress;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrValue;
import decaf.ir.dataflow.Direction;
import decaf.ir.dataflow.copy.CopyQuadruple;
import decaf.ir.dataflow.operand.UnmodifiedOperand;

public class AvailableCopies extends DataFlowAnalysis<CopyQuadruple> {
  // each basicBlock maps to another map of (u -> v) pairs
  public HashMap<BasicBlock, HashMap<IrValue, IrValue>> availableCopies;

  public AvailableCopies(BasicBlock basicBlock) {
    super(basicBlock);
    populateAvailableCopies();
  }

  @Override
  public void computeUniversalSetsOfValues() {
    allTS = new HashSet<>();
    for (BasicBlock basicBlock : basicBlocks)
      allTS.addAll(copy(basicBlock));
  }

  private void populateAvailableCopies() {
    availableCopies = new HashMap<>();
    for (BasicBlock basicBlock : basicBlocks) {
      if (basicBlock.equals(entryBlock)) continue;
      var copiesForBasicBlock = new HashMap<IrValue, IrValue>();
      for (CopyQuadruple copyQuadruple : in(basicBlock)) {
        copiesForBasicBlock.put(
            copyQuadruple.u(),
            copyQuadruple.v()
        );
      }
      availableCopies.put(
          basicBlock,
          copiesForBasicBlock
      );
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
      out.put(
          basicBlock,
          new HashSet<>()
      ); // all copies are available at initialization time
      in.put(
          basicBlock,
          new HashSet<>(allTS)
      );
    }
    // IN[entry] = ∅
    in.put(
        entryBlock,
        new HashSet<>()
    );
    out.put(
        entryBlock,
        copy(entryBlock)
    );
  }

  @Override
  public void runWorkList() {
    var workList = new ArrayDeque<>(basicBlocks);
    workList.remove(entryBlock);

    while (!workList.isEmpty()) {
      final var B = workList.pop();
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
              copy(B),
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

  @Override
  public Set<CopyQuadruple> meet(BasicBlock basicBlock) {
    if (basicBlock.getPredecessors()
                  .isEmpty())
      return Collections.emptySet();

    var inSet = in(basicBlock);

    for (BasicBlock block : basicBlock.getPredecessors())
      inSet.retainAll(out(block));
    return inSet;
  }

  private HashSet<CopyQuadruple> copy(BasicBlock basicBlock) {
    var copyQuadruples = new HashSet<CopyQuadruple>();

    for (StoreInstruction storeInstruction : basicBlock.getStoreInstructions()) {
      // check to see whether any existing assignments are invalidated by this one
      if (!(storeInstruction.getDestination() instanceof IrMemoryAddress)) {
        final var resulLocation = storeInstruction.getDestination();
        // remove any copy's where u, or v are being reassigned by "resultLocation"
        copyQuadruples.removeIf(copyQuadruple -> copyQuadruple.contains(resulLocation));
      }
      var computation = storeInstruction.getOperand();
      if (computation instanceof UnmodifiedOperand) {
        IrValue name = ((UnmodifiedOperand) computation).irValue;
        copyQuadruples.add(new CopyQuadruple((IrSsaRegister) storeInstruction.getDestination(),
                                             name,
                                             basicBlock
                                                 .getCopyOfInstructionList()
                                                 .indexOf(storeInstruction),
                                             basicBlock
        ));
      }

    }
    return copyQuadruples;
  }


  private Set<CopyQuadruple> kill(BasicBlock basicBlock) {
    var superSet = new HashSet<>(allTS);
    var killedCopyQuadruples = new HashSet<CopyQuadruple>();
    for (StoreInstruction storeInstruction : basicBlock.getStoreInstructions()) {
      if (!(storeInstruction.getDestination() instanceof IrMemoryAddress)) {
        final var resulLocation = storeInstruction.getDestination();
        for (CopyQuadruple copyQuadruple : superSet) {
          if (copyQuadruple.basicBlock() != basicBlock && copyQuadruple.contains(resulLocation)) {
            killedCopyQuadruples.add(copyQuadruple);
          }
        }
      }
    }
    return killedCopyQuadruples;
  }
}
