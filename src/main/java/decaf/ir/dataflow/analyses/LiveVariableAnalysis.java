package decaf.ir.dataflow.analyses;

import com.google.common.collect.Sets;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.dataflow.Direction;
import decaf.ir.dataflow.usedef.Def;
import decaf.ir.dataflow.usedef.Use;
import decaf.ir.dataflow.usedef.UseDef;
import decaf.ir.names.IrValue;
import decaf.ir.names.IrValuePredicates;

public class LiveVariableAnalysis extends DataFlowAnalysis<UseDef> {
  public LiveVariableAnalysis(BasicBlock basicBlock) {
    super(basicBlock);
  }

  public Set<IrValue> liveOut(BasicBlock basicBlock) {
    // the set of variables actually needed later in the program
    return out
        .get(basicBlock)
        .stream()
        .map(useDef -> useDef.variable)
        .collect(Collectors.toSet());
  }

  public Set<IrValue> liveIn(BasicBlock basicBlock) {
    return in
        .get(basicBlock)
        .stream()
        .map(useDef -> useDef.variable)
        .collect(Collectors.toSet());
  }

  @Override
  public void computeUniversalSetsOfValues() {
  }

  @Override
  public Set<UseDef> transferFunction(UseDef domainElement) {
    return null;
  }

  @Override
  public Direction direction() {
    return Direction.BACKWARDS;
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
          new HashSet<>()
      );
    }
    in.put(
        exitBlock,
        use(exitBlock)
    );
  }

  @Override
  public void runWorkList() {
    var workList = new ArrayDeque<>(basicBlocks);
    workList.remove(entryBlock);

    while (!workList.isEmpty()) {
      final BasicBlock B = workList.pop();
      var oldIn = in(B);

      // OUT[B] = intersect OUT[s] for all s in successors
      out.put(
          B,
          meet(B)
      );

      // IN[B] = USE[B] ∪ (IN[B] - DEF[B])
      in.put(
          B,
          Sets.union(
              use(B),
              Sets.difference(
                  out(B),
                  def(B)
              )
          )
      );

      if (!in(B).equals(oldIn)) {
        workList.addAll(B.getPredecessors());
      }
    }
  }

  // an instruction makes a irAssignableValue "live" it references it
  private Set<UseDef> use(BasicBlock basicBlock) {
    var useSet = new HashSet<UseDef>();
    for (Instruction instruction : basicBlock.getInstructionListReversed()) {
      if (instruction instanceof HasOperand hasOperand) {
        hasOperand.genOperandIrValuesFiltered(IrValuePredicates.isRegisterAllocatable())
                  .forEach(lValue -> useSet.add(new Use(
                      lValue,
                      instruction
                  )));
      }
    }
    return useSet;
  }

  private Set<UseDef> def(BasicBlock basicBlock) {
    var defSet = new HashSet<UseDef>();
    for (Instruction instruction : basicBlock.getInstructionListReversed()) {
      if (instruction instanceof StoreInstruction storeInstruction) {
        defSet.add(new Def(
            storeInstruction.getDestination(),
            storeInstruction
        ));
      }
      if (instruction instanceof GetAddress getAddress) {
        defSet.add(new Def(
            getAddress.getBaseAddress(),
            getAddress
        ));
      }
    }
    return defSet;
  }


  @Override
  public Set<UseDef> meet(BasicBlock basicBlock) {
    var outSet = new HashSet<UseDef>();
    for (var successor : basicBlock.getSuccessors()) {
      outSet.addAll(in(successor));
    }
    return outSet;
  }
}
