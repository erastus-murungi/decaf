package edu.mit.compilers.dataflow.ssapasses;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.mit.compilers.utils.TarjanSCC.getReversePostOrder;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.codes.UnconditionalBranch;
import edu.mit.compilers.codegen.names.IrGlobal;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.codegen.names.IrMemoryAddress;
import edu.mit.compilers.codegen.names.IrIntegerConstant;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.dataflow.ssapasses.worklistitems.SsaEdge;
import edu.mit.compilers.ssa.Phi;
import edu.mit.compilers.utils.SSAEdgesUtil;
import edu.mit.compilers.utils.Utils;

/**
 * Sparse Conditional Constant Propagation
 */
public class SCCP {
  private final Stack<BasicBlock> flowGraphWorkList = new Stack<>();
  private final Stack<SsaEdge> ssaWorkList = new Stack<>();
  private final Set<BasicBlock> reachableBasicBlocks = new HashSet<>();
  private final Map<IrValue, LatticeElement> latticeValues = new HashMap<>();
  private final Set<BasicBlock> visitedBasicBlocks = new HashSet<>();
  private final Set<SsaEdge> ssaEdgeList = new HashSet<>();

  public SCCP(Method method) {
    checkNotNull(method);
    initializeWorkSets(method);
    runWorkList();
  }

  public void initializeWorkSets(Method method) {
    for (BasicBlock basicBlock : getReversePostOrder(method.entryBlock)) {
      for (Instruction instruction : basicBlock.getInstructionList()) {
        for (IrValue v : instruction.getAllValues()) {
          if (v instanceof IrRegister) {
            latticeValues.put(v, LatticeElement.top());
          } else if (v instanceof IrIntegerConstant numericalConstant) {
            latticeValues.put(v, LatticeElement.constant(numericalConstant.getValue()));
          } else if (v instanceof IrMemoryAddress) {
            latticeValues.put(v, LatticeElement.bottom());
          } else if (v instanceof IrGlobal) {
            latticeValues.put(v, LatticeElement.bottom());
          }
        }
      }
    }
    var ssaEdges = new SSAEdgesUtil(method).getSsaEdges();
    this.ssaEdgeList.addAll(ssaEdges);

    this.ssaWorkList.addAll(ssaEdges);
    this.flowGraphWorkList.add(method.entryBlock);

  }

  private List<SsaEdge> getSsaEdgesForVariable(IrAssignableValue virtualRegister) {
    return ssaEdgeList.stream().filter(ssaEdge -> ssaEdge.getValue().equals(virtualRegister)).toList();
  }

  private void visitPhi(Phi phi) {
    var values = phi.getOperandValues().stream()
                    .map(value -> reachableBasicBlocks.contains(phi.getBasicBlockForV(value)) ? latticeValues.get(value): LatticeElement.top())
                    .collect(Collectors.toList());
    var newLatticeElement = LatticeElement.meet(values);
    if (!newLatticeElement.equals(latticeValues.get(phi.getDestination()))) {
      latticeValues.put(phi.getDestination(), LatticeElement.meet(values));
      ssaWorkList.addAll(getSsaEdgesForVariable(phi.getDestination()));
    }
  }

  private void visitExpression(
      Instruction instruction,
      BasicBlock basicBlock
  ) {
    if (instruction instanceof CopyInstruction copyInstruction) {
      var updated = latticeValues.get(copyInstruction.getValue());
      if (!updated.equals(latticeValues.get(copyInstruction.getDestination()))) {
        latticeValues.put(copyInstruction.getDestination(), updated);
        ssaWorkList.addAll(getSsaEdgesForVariable(copyInstruction.getDestination()));
      }
    } else if (instruction instanceof BinaryInstruction binaryInstruction) {
      var a = latticeValues.get(binaryInstruction.fstOperand);
      var b = latticeValues.get(binaryInstruction.sndOperand);
      if (!a.isBottom() && !b.isBottom()) {
        @NotNull LatticeElement updated;
        if (a.isConstant() && b.isConstant()) {
          var longVal = Utils.symbolicallyEvaluate(String.format("%d %s %d", a.getValue(), binaryInstruction.operator, b.getValue()))
                             .orElseThrow();
          updated = LatticeElement.constant(longVal);
        } else {
          updated = LatticeElement.meet(a, b);
        }
        if (!updated.equals(latticeValues.get(binaryInstruction.getDestination()))) {
          latticeValues.put(binaryInstruction.getDestination(), updated);
          ssaWorkList.addAll(getSsaEdgesForVariable(binaryInstruction.getDestination()));
        }
      }
    } else if (instruction instanceof UnaryInstruction unaryInstruction) {
      var a = latticeValues.get(unaryInstruction.operand);
      if (a.isConstant()) {
        var longVal = Utils.symbolicallyEvaluateUnaryInstruction(unaryInstruction.operator, a.getValue());
        var updated = LatticeElement.constant(longVal);
        if (!updated.equals(latticeValues.get(unaryInstruction.getDestination()))) {
          latticeValues.put(unaryInstruction.getDestination(), updated);
          ssaWorkList.addAll(getSsaEdgesForVariable(unaryInstruction.getDestination()));
        }
      }
    } else if (instruction instanceof ConditionalBranch conditionalBranch) {
      var updated = latticeValues.get(conditionalBranch.getCondition());
      if (updated.isConstant()) {
        if (updated.getValue().equals(1L)) {
          flowGraphWorkList.add(basicBlock.getTrueTarget());
        } else {
          assert updated.getValue().equals(0L);
          flowGraphWorkList.add(basicBlock.getFalseTarget());
        }
      } else {
        flowGraphWorkList.add(basicBlock.getTrueTarget());
        flowGraphWorkList.add(basicBlock.getFalseTarget());
      }
    } else if (instruction instanceof UnconditionalBranch unconditionalBranch) {
      if (!reachableBasicBlocks.contains(unconditionalBranch.getTarget()))
        flowGraphWorkList.add(unconditionalBranch.getTarget());
    }

  }

  public boolean isReachable(BasicBlock basicBlock) {
    return reachableBasicBlocks.contains(basicBlock);
  }

  /**
   * ♦ Use two work lists: SSAWorkList & CFGWorkList:
   * ♦ SSAWorkList determines values.
   * ♦ CFGWorkList governs reachability.
   * ♦ Don’t propagate into operation until its block is reachable.
   */

  public void runWorkList() {
    while (!flowGraphWorkList.isEmpty() || !ssaWorkList.isEmpty()) {
      while (!flowGraphWorkList.isEmpty()) {
        var basicBlock = flowGraphWorkList.pop();
        if (!isReachable(basicBlock)) {
          reachableBasicBlocks.add(basicBlock);
          // (b) Perform Visit-phi for all the phi functions at the destination node
          basicBlock.getPhiFunctions().forEach(this::visitPhi);

          // (c) If only one of the ExecutableFlags associated with the incoming
          //     program flow graph edges is true (i.e. this the first time this
          //     node has been evaluated), then perform VisitExpression for all expressions
          //     in this node.
          if (!visitedBasicBlocks.contains(basicBlock)) {
            basicBlock.getNonPhiInstructions().forEach(instruction -> visitExpression(instruction, basicBlock));
            visitedBasicBlocks.add(basicBlock);
          }

          // (d) If then node only contains one outgoing flow edge, add that edge to the
          //     flowWorkList
          if (basicBlock.hasNoBranchNotNOP()) {
            flowGraphWorkList.add(basicBlock.getSuccessor());
          }
        }
      }
      while (!ssaWorkList.isEmpty()) {
        var ssaEdge = ssaWorkList.pop();

        // (4) If the item is an SSA edge from the SSAWorkList and the destination of that
        //     edge is a phi-function, perform visit-phi

        // (5) If the item is an SSA edge from the SSA Work list and the destination of that
        //     edge is an expression, then examine ExecutableFlags for the program flow edges
        //     reaching that node. If any of them are true, perform VisitExpression.
        //     Otherwise, do nothing.

        if (ssaEdge.use() instanceof Phi) {
          visitPhi((Phi) ssaEdge.use());
        } else if (ssaEdge.basicBlockOfUse().getPredecessors().stream().anyMatch(this::isReachable)) {
          visitExpression(ssaEdge.use(), ssaEdge.basicBlockOfUse());
        }
      }
    }
  }

  public Map<IrValue, LatticeElement> getLatticeValues() {
    return latticeValues;
  }
}
