package edu.mit.compilers.dataflow.ssapasses;

import static edu.mit.compilers.utils.TarjanSCC.correctPredecessors;
import static edu.mit.compilers.utils.TarjanSCC.getReversePostOrder;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.BasicBlockBranchLess;
import edu.mit.compilers.cfg.BasicBlockWithBranch;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.ReturnInstruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.dominator.ImmediateDominator;
import edu.mit.compilers.dataflow.ssapasses.worklistitems.FlowEdge;
import edu.mit.compilers.dataflow.ssapasses.worklistitems.SsaEdge;
import edu.mit.compilers.ssa.Phi;
import edu.mit.compilers.utils.Utils;

public class SCCP {
    public static final String ENTRY_BLOCK_LABEL = "entry";
    Queue<FlowEdge> flowEdges = new ArrayDeque<>();
    Queue<SsaEdge> ssaEdges = new ArrayDeque<>();
    Set<FlowEdge> executable = new HashSet<>();

    Map<Instruction, Map<Value, LatticeElement>> m = new HashMap<>();

    BasicBlockBranchLess entryBlock;

    public SCCP(BasicBlock entryBlock) {
        this.entryBlock = preprocess(entryBlock);
        initializeWorkSets();
        runWorkList();
    }

    private NOP preprocess(BasicBlock entryBlock) {
        var entry = new NOP(ENTRY_BLOCK_LABEL);
        entry.setSuccessor(entryBlock);
        correctPredecessors(entry);
        return entry;
    }

    public Set<LatticeElement> meet(BasicBlock basicBlock) {
        return null;
    }

    public Set<LatticeElement> transferFunction(LatticeElement latticeElement) {
        return null;
    }

    public void initializeWorkSets() {
        for (BasicBlock basicBlock : getReversePostOrder(entryBlock)) {
            for (Instruction instruction : basicBlock.getInstructionList()) {
                var n = new HashMap<Value, LatticeElement>();
                for (Value v : instruction.getAllValues()) {
                    if (v instanceof LValue) {
                        n.put(v, LatticeElement.top());
                    } else if (v instanceof NumericalConstant numericalConstant) {
                        n.put(v, LatticeElement.constant(numericalConstant.getValue()));
                    }
                }
                m.put(instruction, n);
            }
        }
        this.flowEdges.add(new FlowEdge(entryBlock, entryBlock.getSuccessor()));
        getSsaEdges();
    }

    private void visitPhi(Phi phi) {
        var lattices = m.get(phi);
        var values = phi.getOperandValues().stream().map(lattices::get).toList();
        lattices.put(phi.getStore(), LatticeElement.meet(values));
    }

    private void visitExpression(Instruction instruction, BasicBlock basicBlock) {
        var lattices = m.get(instruction);
        if (instruction instanceof CopyInstruction copyInstruction) {
            lattices.put(copyInstruction.getStore(), lattices.get(copyInstruction.getValue()));
        } else if (instruction instanceof BinaryInstruction binaryInstruction) {
            var a = lattices.get(binaryInstruction.fstOperand);
            var b  = lattices.get(binaryInstruction.sndOperand);
            if (!a.isBottom() && !b.isBottom()) {
                if (a.isConstant() && b.isConstant()) {
                    var longVal = Utils.symbolicallyEvaluate(String.format("%d %s %d", a.getValue(), binaryInstruction.operator, b.getValue()));
                    longVal.ifPresent(aLong -> lattices.put(binaryInstruction.getStore(), LatticeElement.constant(aLong)));
                } else {
                    lattices.put(binaryInstruction.getStore(), LatticeElement.meet(a, b));
                }
            }
        } else if (instruction instanceof UnaryInstruction unaryInstruction) {
            var a = lattices.get(unaryInstruction.operand);
            if (a.isConstant()) {
                var longVal = Utils.symbolicallyEvaluate(String.format("%s (%d)", unaryInstruction.operator, a.getValue()));
                longVal.ifPresent(aLong -> lattices.put(unaryInstruction.getStore(), LatticeElement.constant(aLong)));
            }
        } else if (instruction instanceof ConditionalBranch conditionalBranch) {
            var branch = (BasicBlockWithBranch) basicBlock;
            var condition = lattices.get(conditionalBranch.condition);
            if (LatticeElement.meet(condition, LatticeElement.constant(0L)).isBottom()) {
                flowEdges.add(new FlowEdge(branch, branch.getFalseTarget()));
            }
             if (LatticeElement.meet(condition, LatticeElement.constant(1L)).isBottom()) {
                flowEdges.add(new FlowEdge(branch, branch.getTrueTarget()));
            }
        }

    }

    public int countIncomingExecutableEdges(BasicBlock block) {
        int count = 0;
        for (var pred : block.getPredecessors()) {
            if (executable.contains(new FlowEdge(pred, block))) {
                count++;
            }
        }
        return count;
    }

    private boolean isExecutable(FlowEdge flowEdge) {
        return executable.contains(flowEdge);
    }

    private void setExecutable(FlowEdge flowEdge) {
        executable.add(flowEdge);
    }

    private void getSsaEdges() {
        ImmediateDominator immediateDominator = new ImmediateDominator(entryBlock);
        Set<SsaEdge> ssaEdges = new HashSet<>();
        Map<LValue, StoreInstruction> lValueToDefMapping = new HashMap<>();
        for (BasicBlock basicBlock : immediateDominator.preorder()) {

            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof StoreInstruction storeInstruction) {
                    lValueToDefMapping.put(storeInstruction.getStore(), storeInstruction);
                }

                if (instruction instanceof HasOperand hasOperand) {
                    for (LValue lValue : hasOperand.getOperandLValues()) {
                        if (instruction instanceof ReturnInstruction && !lValueToDefMapping.containsKey(lValue))
                            continue;
                        ssaEdges.add(new SsaEdge(lValueToDefMapping.computeIfAbsent(lValue, key -> {
                            throw new IllegalStateException(lValue + " not found" + basicBlock.getInstructionList().toString());
                        }), hasOperand, basicBlock));
                    }
                }
            }
        }
        this.ssaEdges.addAll(ssaEdges);
    }

    public void runWorkList() {
        while (!flowEdges.isEmpty() && !ssaEdges.isEmpty()) {
            while (!flowEdges.isEmpty()) {
                var flowEdge = flowEdges.remove();
                if (!isExecutable(flowEdge)) {
                    setExecutable(flowEdge);
                    var x = flowEdge.start();
                    var y = flowEdge.sink();
                    // (b) Perform Visit-phi for all the phi functions at the destination node
                    y.getPhiFunctions()
                     .forEach(this::visitPhi);

                    // (c) If only one of the ExecutableFlags associated with the incoming
                    //     program flow graph edges is true (i.e. this the first time this
                    //     node has been evaluated), then perform VisitExpression for all expressions
                    //     in this node.
                    if (countIncomingExecutableEdges(x) == 1) {
                        y.getNonPhiInstructions()
                         .forEach(instruction -> visitExpression(instruction, y));
                    }

                    // (d) If then node only contains one outgoing flow edge, add that edge to the
                    //     flowWorkList
                    if (y instanceof BasicBlockBranchLess) {
                        flowEdges.add(new FlowEdge(x, ((BasicBlockBranchLess) x).getSuccessor()));
                    }
                }
            }
            while (!ssaEdges.isEmpty()) {
                var ssaEdge = ssaEdges.remove();

                // (4) If the item is an SSA edge from the SSAWorkList and the destination of that
                //     edge is a phi-function, perform visit-phi

                // (5) If the item is an SSA edge from the SSA Work list and the destination of that
                //     edge is an expression, then examine ExecutableFlags for the program flow edges
                //     reaching that node. If any of them are true, perform VisitExpression.
                //     Otherwise, do nothing.

                if (ssaEdge.useIsPhi()) {
                    visitPhi((Phi) ssaEdge.use());
                } else if (countIncomingExecutableEdges(ssaEdge.useSite()) > 0) {
                    visitExpression(ssaEdge.use(), ssaEdge.useSite());
                }
            }
        }
    }
}
