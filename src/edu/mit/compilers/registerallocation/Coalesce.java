package edu.mit.compilers.registerallocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.utils.CLI;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.TarjanSCC;
import edu.mit.compilers.utils.UnionFind;

public class Coalesce {
    public static void doIt(Method method, ProgramIr programIr) {
        var changesHappened = false;
        while (!changesHappened) {
            var liveIntervalsUtil = new LiveIntervalsUtil(method, programIr);
            if (CLI.debug)
                liveIntervalsUtil.prettyPrintLiveIntervals(method);
            var interferenceGraph = new InterferenceGraph(liveIntervalsUtil, method);
            var unionFind = new UnionFind<>(interferenceGraph.getMoveNodes());
            for (var pair : interferenceGraph.getUniqueMoveEdges()) {
                unionFind.union(pair.first(), pair.second());
            }
            var sets = unionFind.toSets();
            var allUses = sets.stream()
                    .filter(nodes -> nodes.size() > 1)
                    .map(nodes -> nodes.stream().filter(liveInterval -> liveInterval.irAssignableValue() instanceof IrRegister)
                            .map(liveInterval -> (IrRegister) liveInterval
                                    .irAssignableValue().copy())
                            .collect(Collectors.toUnmodifiableSet()))
                    .toList();
            if (allUses.isEmpty())
                break;
            for (var uses : allUses) {
                changesHappened = changesHappened | renameAllUses(uses, method);
            }
        }
        if (CLI.debug)
            new LiveIntervalsUtil(method, programIr).prettyPrintLiveIntervals(method);
    }

    private static boolean renameAllUses(Collection<IrRegister> uses, Method method) {
        var changesHappened = false;
        if (uses.size() < 2)
            return false;
        var basicBlocks = TarjanSCC.getReversePostOrder(method.entryBlock);
        var newName = uses.stream()
                .min(Comparator.comparing(Object::toString))
                .orElseThrow().copy();
        for (BasicBlock basicBlock : basicBlocks) {
            List<Instruction> instructions = new ArrayList<>();
            for (Instruction instruction : basicBlock.getInstructionList()) {
                for (IrRegister irRegister : instruction.getAllVirtualRegisters()) {
                    if (uses.contains(irRegister)) {
                        changesHappened = true;
                        irRegister.renameForSsa(newName);
                    }
                }
                if (instruction instanceof CopyInstruction copyInstruction) {
                    if (copyInstruction.getDestination().equals(copyInstruction.getValue()))
                        continue;
                }
                instructions.add(instruction);
            }
            basicBlock.getInstructionList().reset(instructions);
        }
        return changesHappened;
    }
}
