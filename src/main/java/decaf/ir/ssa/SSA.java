package decaf.ir.ssa;

import static com.google.common.base.Preconditions.checkState;
import static decaf.shared.StronglyConnectedComponentsTarjan.getReversePostOrder;
import static decaf.shared.Utils.genIrSsaRegistersIn;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.dataflow.analyses.LiveVariableAnalysis;
import decaf.ir.dataflow.dominator.DominatorTree;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrValue;
import decaf.shared.Pair;
import decaf.shared.ProgramIr;
import decaf.shared.UnionFind;
import decaf.shared.Utils;
import decaf.synthesis.regalloc.InterferenceGraph;
import decaf.synthesis.regalloc.LiveIntervalsManager;

public class SSA {
  private SSA() {
  }

  private static void buildCfgGuava(List<BasicBlock> basicBlocks) {
    // Gr
    MutableGraph<BasicBlock> graph = GraphBuilder.directed()
                                                 .build();
    basicBlocks.forEach(basicBlock -> {
      graph.addNode(basicBlock);
      basicBlock.getSuccessors()
                .forEach(successor -> {
                  graph.addNode(successor);
                  graph.putEdge(
                      basicBlock,
                      successor
                  );
                });
    });
  }

  public static void construct(Method method) {
    var entryBlock = method.getEntryBlock();
    var basicBlocks = getReversePostOrder(entryBlock);
//    buildCfgGuava(basicBlocks);
    var dominatorTree = new DominatorTree(entryBlock);
    placePhiFunctions(
        entryBlock,
        basicBlocks,
        dominatorTree
    );
    renameVariables(
        method,
        dominatorTree,
        basicBlocks
    );
    verifySsa(basicBlocks);
  }

  private static void renameMethodArgs(Method method) {
    for (var V : method.getParameterNames()) {
      V.renameForSsa(0);
    }
  }

  public static void deconstruct(
      Method method,
      ProgramIr programIr
  ) {

    var entryBlock = method.getEntryBlock();
    var basicBlocks = getReversePostOrder(entryBlock);
    var immediateDominator = new DominatorTree(entryBlock);
    verifySsa(basicBlocks);
    deconstructSsa(
        entryBlock,
        basicBlocks,
        immediateDominator
    );
    coalesce(
        method,
        programIr
    );
  }

  private static Set<BasicBlock> getBasicBlocksModifyingVariable(
      IrSsaRegister V,
      List<BasicBlock> basicBlocks
  ) {
    return basicBlocks.stream()
                      .filter(basicBlock -> basicBlock.getStoreInstructions()
                                                      .stream()
                                                      .map(StoreInstruction::getDestination)
                                                      .anyMatch(abstractName -> abstractName.equals(V)))
                      .collect(Collectors.toUnmodifiableSet());
  }

  private static Set<IrSsaRegister> getStoreLocations(BasicBlock X) {
    return X.getStoreInstructions()
            .stream()
            .map(StoreInstruction::getDestination)
            .filter(lValue -> lValue instanceof IrSsaRegister)
            .map(lValue -> (IrSsaRegister) lValue)
            .map(IrSsaRegister::copy)
            .collect(Collectors.toSet());
  }

  private static void initialize(
      Collection<IrSsaRegister> allVariables,
      Map<IrSsaRegister, Stack<Integer>> stacks,
      Map<IrSsaRegister, Integer> counters
  ) {
    allVariables.stream()
                .map(IrSsaRegister::copy)
                .forEach(a -> {
                  counters.put(
                      a,
                      0
                  );
                  stacks.put(
                      a,
                      new Stack<>()
                  );
                  stacks.get(a)
                        .add(0);
                });
  }

  private static void genName(
      IrSsaRegister V,
      Map<IrSsaRegister, Integer> counters,
      Map<IrSsaRegister, Stack<Integer>> stacks
  ) {
    var i = counters.get(V);
    var copyV = (IrSsaRegister) V.copy();
    V.renameForSsa(i);
    stacks.get(copyV)
          .push(i);
    counters.put(
        copyV,
        i + 1
    );
  }

  private static void rename(
      BasicBlock X,
      DominatorTree dominatorTree,
      Map<IrSsaRegister, Integer> counters,
      Map<IrSsaRegister, Stack<Integer>> stacks
  ) {
    final var stores = getStoreLocations(X);

    for (Phi phi : X.getPhiFunctions()) {
      genName(
          (IrSsaRegister) phi.getDestination(),
          counters,
          stacks
      );
    }

    for (Instruction instruction : X.getInstructionList()) {
      if (instruction instanceof Phi) continue;
      if (instruction instanceof HasOperand hasOperand) {
        var Vs = hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class);
        for (var V : Vs) {
          if (stacks.get(V) == null) throw new IllegalStateException(V.toString());
          V.renameForSsa(stacks.get(V)
                               .peek());
        }
      }
      if (instruction instanceof StoreInstruction) {
        var V = ((StoreInstruction) instruction).getDestination();
        if (V instanceof IrSsaRegister irSsaRegister) {
          genName(
              irSsaRegister,
              counters,
              stacks
          );
        }
      }
    }

    for (var Y : X.getSuccessors()) {
      for (Phi phi : Y.getPhiFunctions()) {
        var V = phi.getVariableForB(X);
        if (V instanceof IrSsaRegister irSsaRegister) {
          if (stacks.get(V)
                    .isEmpty()) continue;
          irSsaRegister.renameForSsa(stacks.get(V)
                                           .peek());
        }
      }
    }

    for (var C : dominatorTree.getChildren(X)) {
      rename(
          C,
          dominatorTree,
          counters,
          stacks
      );
    }

    for (var store : stores) {
      stacks.get(store)
            .pop();
    }
  }

  private static void initializeForSsaDestruction(
      List<BasicBlock> basicBlocks,
      HashMap<IrSsaRegister, Stack<IrSsaRegister>> stacks
  ) {
    genIrSsaRegistersIn(basicBlocks).stream()
                                    .map(IrSsaRegister::copy)
                                    .forEach(a -> {
                                      stacks.put(
                                          a,
                                          new Stack<>()
                                      );
                                      stacks.get(a)
                                            .add(a.copy());
                                    });
  }

  private static int getNCopiesOfV(
      BasicBlock X,
      Collection<BasicBlock> basicBlocksModifyingV
  ) {
    int nCopiesOfV = (int) X.getPredecessors()
                            .stream()
                            .filter(basicBlocksModifyingV::contains)
                            .count();
    return nCopiesOfV;
  }

  private static int genNCopiesOfV(
      BasicBlock X,
      Collection<BasicBlock> basicBlocksModifyingV
  ) {
    // we find the number of predecessors of V which can be reached by all the blocks modifying V
    return (int) X.getPredecessors()
                  .stream()
                  .filter(predecessor -> basicBlocksModifyingV.stream()
                                                              .anyMatch(modifier -> Utils.isReachable(
                                                                  predecessor,
                                                                  modifier
                                                              )))
                  .count();
  }

  private static void addPhiNodeForVatY(
      IrSsaRegister V,
      BasicBlock X,
      Collection<BasicBlock> basicBlocksModifyingV
  ) {
    var copiesOfV = genNCopiesOfV(
        X,
        basicBlocksModifyingV
    );
    if (copiesOfV > 1) {
      var blockToVariable = new HashMap<BasicBlock, IrValue>();
      for (var P : X.getPredecessors()) {
        blockToVariable.put(
            P,
            V.copy()
        );
      }
      X.getInstructionList()
       .add(
           0,
           new Phi(
               V.copy(),
               blockToVariable
           )
       );
    }
  }

  /**
   * Places phi functions to create a pruned SSA form
   *
   * @param entryBlock the first basic block of the function
   */
  private static void placePhiFunctions(
      BasicBlock entryBlock,
      List<BasicBlock> basicBlocks,
      DominatorTree dominatorTree
  ) {
    var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);

    for (var V : genIrSsaRegistersIn(basicBlocks)) {
      var hasAlready = new HashSet<BasicBlock>();
      var everOnWorkList = new HashSet<BasicBlock>();
      var workList = new ArrayDeque<BasicBlock>();

      var basicBlocksModifyingV = getBasicBlocksModifyingVariable(
          V,
          basicBlocks
      );
      for (var basicBlock : basicBlocksModifyingV) {
        everOnWorkList.add(basicBlock);
        workList.add(basicBlock);
      }
      while (!workList.isEmpty()) {
        var X = workList.pop();
        for (var Y : dominatorTree.getDominanceFrontier(X)) {
          if (!hasAlready.contains(Y)) {
            // we only insert a phi node for irAssignableValue V if is live on entry to X
            if (liveVariableAnalysis.liveIn(X)
                                    .contains(V)) {
              addPhiNodeForVatY(
                  V,
                  Y,
                  basicBlocksModifyingV
              );
            }
            hasAlready.add(Y);
            if (!everOnWorkList.contains(Y)) {
              everOnWorkList.add(Y);
              workList.add(Y);
            }
          }
        }
      }
    }
  }

  private static void renameVariables(
      Method method,
      DominatorTree dominatorTree,
      List<BasicBlock> basicBlocks
  ) {
    var stacks = new HashMap<IrSsaRegister, Stack<Integer>>();
    var counters = new HashMap<IrSsaRegister, Integer>();
    var variables = genIrSsaRegistersIn(basicBlocks);
    initialize(
        variables,
        stacks,
        counters
    );
    rename(
        method.getEntryBlock(),
        dominatorTree,
        counters,
        stacks
    );
    renameMethodArgs(method);
  }

  private static void verifySsa(List<BasicBlock> basicBlocks) {
    var seen = new HashSet<IrSsaRegister>();
    for (var B : basicBlocks) {
      for (var instruction : B.getInstructionList()) {
        if (instruction instanceof HasOperand hasOperand) {
          checkState(
              hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class)
                        .stream()
                        .allMatch(irSsaRegister -> irSsaRegister.getVersionNumber() != null),
              hasOperand + " has non ssa variables"
          );
        }
        if (instruction instanceof StoreInstruction storeInstruction) {
          if (storeInstruction.getDestination() instanceof IrSsaRegister irSsaRegisterDestination) {
            if (seen.contains(irSsaRegisterDestination)) {
              throw new IllegalStateException(storeInstruction.getDestination() + " store redefined");
            } else {
              seen.add(irSsaRegisterDestination);
            }
            if (storeInstruction instanceof Phi) {
              var numPhiOperands = storeInstruction.genOperandIrValuesSurface()
                                                   .size();
              if (numPhiOperands < 1) {
                throw new IllegalStateException(
                    storeInstruction.syntaxHighlightedToString() + " has " + numPhiOperands + " operands");
              }
            }
          }
        }
      }
    }
  }

  public static void verifySsa(Method method) {
    var basicBlocks = getReversePostOrder(method.getEntryBlock());
    SSA.verifySsa(basicBlocks);
  }

  private static void deconstructSsa(
      BasicBlock entryBlock,
      List<BasicBlock> basicBlocks,
      DominatorTree dominatorTree
  ) {
    var stacks = new HashMap<IrSsaRegister, Stack<IrSsaRegister>>();
    initializeForSsaDestruction(
        basicBlocks,
        stacks
    );
    insertCopies(
        entryBlock,
        dominatorTree,
        new LiveVariableAnalysis(entryBlock),
        stacks
    );
    removePhiNodes(basicBlocks);
  }

  private static void removePhiNodes(List<BasicBlock> basicBlocks) {
    for (BasicBlock basicBlock : basicBlocks) {
      basicBlock.getInstructionList()
                .reset(basicBlock.getInstructionList()
                                 .stream()
                                 .filter(instruction -> !(instruction instanceof Phi))
                                 .collect(Collectors.toList()));
    }
  }

  private static void insertCopies(
      BasicBlock basicBlock,
      DominatorTree dominatorTree,
      LiveVariableAnalysis liveVariableAnalysis,
      Map<IrSsaRegister, Stack<IrSsaRegister>> stacks
  ) {
    var pushed = new ArrayList<IrSsaRegister>();

    for (Instruction instruction : basicBlock.getInstructionList()) {
      if (instruction instanceof Phi) continue;
      if (instruction instanceof HasOperand hasOperand) {
        var Vs = hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class);
        for (var V : Vs) {
          if (instruction instanceof StoreInstruction storeInstruction) {
            if (storeInstruction.getDestination()
                                .equals(stacks.get(V)
                                              .peek())) continue;
          }
          V.renameForSsa(stacks.get(V)
                               .peek());
        }
      }
    }

    scheduleCopies(
        basicBlock,
        liveVariableAnalysis.liveOut(basicBlock),
        stacks,
        pushed
    );
    for (var child : dominatorTree.getChildren(basicBlock)) {
      insertCopies(
          child,
          dominatorTree,
          liveVariableAnalysis,
          stacks
      );
    }

    for (var name : pushed) {
      stacks.get(name)
            .pop();
    }
    pushed.clear();
  }


  private static void scheduleCopies(
      BasicBlock basicBlock,
      Set<IrValue> liveOut,
      Map<IrSsaRegister, Stack<IrSsaRegister>> stacks,
      ArrayList<IrSsaRegister> pushed
  ) {
    /* Pass One: Initialize the data structures */
    Stack<Pair<IrValue, IrSsaRegister>> copySet = new Stack<>();
    Stack<Pair<IrValue, IrSsaRegister>> workList = new Stack<>();
    Map<IrValue, IrValue> map = new HashMap<>();
    Set<IrValue> usedByAnother = new HashSet<>();
    Map<IrValue, BasicBlock> phiDstToOwnerBasicBlockMapping = new HashMap<>();

    for (var successor : basicBlock.getSuccessors()) {
      for (var phi : successor.getPhiFunctions()) {
        var src = phi.getVariableForB(basicBlock);
        var dst = phi.getDestination();
        if (dst instanceof IrSsaRegister dstVirtual) {
          copySet.add(new Pair<>(
              src,
              dstVirtual
          ));
          map.put(
              src,
              src
          );
          map.put(
              dst,
              dst
          );
          usedByAnother.add(src);
          phiDstToOwnerBasicBlockMapping.put(
              dst,
              successor
          );
        }
      }
    }


    /* Pass Two: Set up the workList of initial copies */
    for (var srcDest : new ArrayList<>(copySet)) {
      var dst = srcDest.second();
      if (!usedByAnother.contains(dst)) {
        workList.add(srcDest);
        copySet.remove(srcDest);
      }
    }

    /* Pass Three: Iterate over the workList, inserting copies */
    while (!workList.isEmpty() || !copySet.isEmpty()) {
      while (!workList.isEmpty()) {
        var srcDest = workList.pop();
        var src = srcDest.first();
        var dst = srcDest.second();
        if (liveOut.contains(dst) && !src.equals(dst)) {
          var temp = (IrSsaRegister) IrSsaRegister.gen(dst.getType());
          var copyInstruction = CopyInstruction.noAstConstructor(
              temp,
              dst.copy()
          );
          var dstOwner = Objects.requireNonNull(
              phiDstToOwnerBasicBlockMapping.get(dst),
              "dest " + dst + " does not have a source basic block"
          );
          dstOwner.getInstructionList()
                  .add(
                      0,
                      copyInstruction
                  );
          stacks.get(dst)
                .push(temp);
          stacks.put(
              temp,
              new Stack<>()
          );
          stacks.get(temp)
                .add(temp.copy());
          pushed.add(dst);
        }
        var copyInstruction = CopyInstruction.noAstConstructor(
            dst,
            map.get(src)
        );
        basicBlock.addInstructionToTail(copyInstruction);
        map.put(
            src,
            dst
        );

        var subWorkList = getPairsWhoseDestinationEquals(
            src,
            copySet
        );
        workList.addAll(subWorkList);
        subWorkList.forEach(copySet::remove);
      }
      if (!copySet.isEmpty()) {
        var srcDest = copySet.pop();
        var dst = srcDest.second();
        var temp = IrSsaRegister.gen(dst.getType());
        var copyInstruction = CopyInstruction.noAstConstructor(
            dst.copy(),
            temp
        );
        basicBlock.addInstructionToTail(copyInstruction);
        map.put(
            dst,
            temp
        );
        workList.add(srcDest);
      }
    }
  }

  private static List<Pair<IrValue, IrSsaRegister>> getPairsWhoseDestinationEquals(
      IrValue src,
      Collection<Pair<IrValue, IrSsaRegister>> pairs
  ) {
    var results = new ArrayList<Pair<IrValue, IrSsaRegister>>();
    for (var pair : pairs) {
      var dst = pair.second();
      if (dst.equals(src)) results.add(pair);
    }
    return results;
  }

  public static void coalesce(
      Method method,
      ProgramIr programIr
  ) {
    while (true) {
      var changesHappened = false;
      var liveIntervalsUtil = new LiveIntervalsManager(
          method,
          programIr
      );
      var interferenceGraph = new InterferenceGraph(
          liveIntervalsUtil,
          method
      );
      var unionFind = new UnionFind<>(interferenceGraph.getMoveNodes());
      for (var pair : interferenceGraph.getUniqueMoveEdges()) {
        unionFind.union(
            pair.first(),
            pair.second()
        );
      }
      var sets = unionFind.toSets();
      var allUses = sets.stream()
                        .filter(nodes -> nodes.size() > 1)
                        .map(nodes -> nodes.stream()
                                           .filter(liveInterval -> liveInterval.irSsaRegister() instanceof IrSsaRegister)
                                           .map(liveInterval -> (IrSsaRegister) liveInterval.irSsaRegister()
                                                                                            .copy())
                                           .collect(Collectors.toUnmodifiableSet()))
                        .toList();
      if (allUses.isEmpty()) break;
      for (var uses : allUses) {
        if (uses.size() >= 2) {
          changesHappened = changesHappened | renameAllUses(
              uses,
              method
          );
        }
      }
      if (!changesHappened) break;
    }
  }

  private static boolean renameAllUses(
      Collection<IrSsaRegister> uses,
      Method method
  ) {
    var changesHappened = false;
    var basicBlocks = getReversePostOrder(method.getEntryBlock());
    var newName = uses.stream()
                      .min(Comparator.comparing(Object::toString))
                      .orElseThrow()
                      .copy();
    for (BasicBlock basicBlock : basicBlocks) {
      List<Instruction> instructions = new ArrayList<>();
      for (Instruction instruction : basicBlock.getInstructionList()) {
        for (IrSsaRegister irSsaRegister : instruction.genIrValuesFiltered(IrSsaRegister.class)) {
          if (uses.contains(irSsaRegister)) {
            changesHappened = true;
            irSsaRegister.renameForSsa(newName);
          }
        }
        if (instruction instanceof CopyInstruction copyInstruction) {
          if (copyInstruction.getDestination()
                             .equals(copyInstruction.getValue())) continue;
        }
        instructions.add(instruction);
      }
      basicBlock.getInstructionList()
                .reset(instructions);
    }
    return changesHappened;
  }

}
