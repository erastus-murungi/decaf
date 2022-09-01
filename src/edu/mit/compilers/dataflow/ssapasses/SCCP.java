package edu.mit.compilers.dataflow.ssapasses;

import static edu.mit.compilers.dataflow.analyses.DataFlowAnalysis.correctPredecessors;
import static edu.mit.compilers.dataflow.analyses.DataFlowAnalysis.getReversePostOrder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.BasicBlockBranchLess;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.dataflow.dominator.ImmediateDominator;
import edu.mit.compilers.dataflow.ssapasses.worklistitems.FlowEdge;
import edu.mit.compilers.dataflow.ssapasses.worklistitems.SsaEdge;
import edu.mit.compilers.ssa.Phi;

public class SCCP {
    public static final String ENTRY_BLOCK_LABEL = "entry";
    Queue<FlowEdge> flowEdges = new ArrayDeque<>();
    Queue<SsaEdge> ssaEdges = new ArrayDeque<>();
    Set<FlowEdge> executable = new HashSet<>();

    Map<Instruction, List<LatticeCell>> m = new HashMap<>();
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
                m.put(instruction, new ArrayList<>());
                for (Value v : instruction.getAllValues()) {
                    if (v instanceof LValue) {
                        m.get(instruction)
                         .add(new LatticeCell(v, LatticeElement.top()));
                    }
                }
            }
        }
        this.flowEdges.add(new FlowEdge(entryBlock, entryBlock.getSuccessor()));
        getSsaEdges();
    }

    private void visitPhi(Phi phi) {
    }

    private void visitExpression(BasicBlock basicBlock, Instruction instruction) {

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
                if (instruction instanceof StoreInstruction) {
                    StoreInstruction storeInstruction = (StoreInstruction) instruction;
                    lValueToDefMapping.put(storeInstruction.getStore(), storeInstruction);
                }
                if (instruction instanceof HasOperand) {
                    HasOperand hasOperand = (HasOperand) instruction;
                    for (LValue lValue : hasOperand.getOperandLValues()) {
                        ssaEdges.add(new SsaEdge(lValueToDefMapping.computeIfAbsent(lValue, key -> {
                            throw new IllegalStateException(lValue + " not found");
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
                    var x = flowEdge.getStart();
                    var y = flowEdge.getSink();
                    // (b) Perform Visit-phi for all the phi functions at the destination node
                    y.getPhiFunctions()
                     .forEach(this::visitPhi);

                    // (c) If only one of the ExecutableFlags associated with the incoming
                    //     program flow graph edges is true (i.e. this the first time this
                    //     node has been evaluated), then perform VisitExpression for all expressions
                    //     in this node.
                    if (countIncomingExecutableEdges(x) == 1) {
                        y.getNonPhiInstructions()
                         .forEach(instruction -> visitExpression(y, instruction));
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
                    visitPhi((Phi) ssaEdge.getUse());
                } else if (countIncomingExecutableEdges(ssaEdge.getUseSite()) > 0) {
                    visitExpression(ssaEdge.getUseSite(), ssaEdge.getUse());
                }
            }
        }
    }
}
