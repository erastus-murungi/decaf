package edu.mit.compilers.dataflow.analyses;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.dataflow.Direction;
import edu.mit.compilers.dataflow.usedef.Def;
import edu.mit.compilers.dataflow.usedef.Use;
import edu.mit.compilers.dataflow.usedef.UseDef;
import edu.mit.compilers.utils.SetUtils;

import java.util.*;
import java.util.stream.Collectors;

public class LiveVariableAnalysis extends DataFlowAnalysis<UseDef> {
    public LiveVariableAnalysis(BasicBlock basicBlock) {
        super(basicBlock);
    }

    public Set<Value> liveOut(BasicBlock basicBlock) {
        // the set of variables actually needed later in the program
        return out
                .get(basicBlock)
                .stream()
                .map(useDef -> useDef.variable)
                .collect(Collectors.toSet());
    }

    public Set<Value> liveIn(BasicBlock basicBlock) {
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
            out.put(basicBlock, new HashSet<>()); // all copies are available at initialization time
            in.put(basicBlock, new HashSet<>());
        }
        in.put(exitBlock, use(exitBlock));
    }

    @Override
    public void runWorkList() {
        var workList = new ArrayDeque<>(basicBlocks);
        workList.remove(entryBlock);

        while (!workList.isEmpty()) {
            final BasicBlock B = workList.pop();
            var oldIn = in(B);

            // OUT[B] = intersect OUT[s] for all s in successors
            out.put(B, meet(B));

            // IN[B] = USE[B] ∪ (IN[B] - DEF[B])
            in.put(B, SetUtils.union(use(B), SetUtils.difference(out(B), def(B))));

            if (!in(B).equals(oldIn)) {
                workList.addAll(B.getPredecessors());
            }
        }
    }

    // an instruction makes a variable "live" it references it
    private Set<UseDef> use(BasicBlock basicBlock) {
        var useSet = new HashSet<UseDef>();
        for (Instruction instruction : basicBlock.getInstructionListReversed()) {
            if (instruction instanceof HasOperand hasOperand) {
                hasOperand.getOperandScalarVariables()
                        .forEach(lValue -> useSet.add(new Use(lValue, instruction)));
            }
        }
        return useSet;
    }

    private Set<UseDef> def(BasicBlock basicBlock) {
        var defSet = new HashSet<UseDef>();
        for (Instruction instruction : basicBlock.getInstructionListReversed()) {
            if (instruction instanceof StoreInstruction storeInstruction) {
                if (!(storeInstruction.getDestination() instanceof MemoryAddress)) {
                    defSet.add(new Def(storeInstruction));
                }
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
