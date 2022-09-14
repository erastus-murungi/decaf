package edu.mit.compilers.registerallocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.utils.Pair;

public class InterferenceGraph {
    HashMap<LiveInterval, Set<LiveInterval>> graph = new HashMap<>();
    Map<LiveInterval, Set<LiveInterval>> isMoveEdge = new HashMap<>();

    public InterferenceGraph(LiveIntervalsUtil liveIntervalsUtil, Method method) {
        var liveIntervals = new ArrayList<>(liveIntervalsUtil.getLiveIntervals(method));
        var varToLivIntervals = liveIntervalsUtil.getValueToLiveIntervalMappingForMethod(method);
        if (liveIntervals.isEmpty()) return;

        for (int i = 0; i < liveIntervals.size(); i++) {
            for (int j = i + 1; j < liveIntervals.size(); j++) {
                var a = (liveIntervals.get(i));
                var b = (liveIntervals.get(j));
                if (LiveIntervalsUtil.liveIntervalsInterfere(a, b)) {
                    insertEdge(a, b);
                }
            }
        }
        var instructionList = liveIntervals.stream()
                                           .findAny()
                                           .orElseThrow()
                                           .instructionList();
        var copyInstructions = instructionList.stream()
                                              .filter(instruction -> (instruction instanceof CopyInstruction))
                                              .map(instruction -> (CopyInstruction) instruction)
                                              .filter(copyInstruction -> copyInstruction.getValue() instanceof IrRegister)
                                              .collect(Collectors.toUnmodifiableSet());
        for (var copyInstruction : copyInstructions) {
            var a = (varToLivIntervals.get(copyInstruction.getDestination()));
            var b = (varToLivIntervals.get(copyInstruction.getValue()));
            if (!LiveIntervalsUtil.liveIntervalsInterfere(a, b)) {
                insertMoveEdge(
                        (varToLivIntervals.get(copyInstruction.getDestination())),
                        (varToLivIntervals.get(copyInstruction.getValue())));
            }
        }
    }

    private static Collection<Pair<LiveInterval, LiveInterval>> getUniqueEdgesHelper(Map<LiveInterval, Set<LiveInterval>> g) {
        var allEdges = new HashSet<Pair<LiveInterval, LiveInterval>>();
        for (var entry : g.entrySet()) {
            for (var node2 : entry.getValue()) {
                var a = new Pair<>(entry.getKey(), node2);
                var b = new Pair<>(node2, entry.getKey());
                if (!allEdges.contains(a) && !allEdges.contains(b))
                    allEdges.add(a);
            }
        }
        return allEdges;
    }

    public void insertEdge(LiveInterval a, LiveInterval b) {
        graph.computeIfAbsent(a, k -> new HashSet<>())
             .add(b);
        graph.computeIfAbsent(b, k -> new HashSet<>())
             .add(a);
    }

    public void insertMoveEdge(LiveInterval a, LiveInterval b) {
        isMoveEdge.computeIfAbsent(a, k -> new HashSet<>())
                  .add(b);
        isMoveEdge.computeIfAbsent(b, k -> new HashSet<>())
                  .add(a);
    }

    public boolean isMoveEdge(LiveInterval a, LiveInterval b) {
        if (isMoveEdge.get(a) == null || isMoveEdge.get(b) == null)
            return false;
        return isMoveEdge.get(a)
                         .contains(b) && isMoveEdge.get(b)
                                                   .contains(a);
    }

    public boolean containsEdge(LiveInterval a, LiveInterval b) {
        return graph.get(a)
                    .contains(b) && graph.get(b)
                                         .contains(a);
    }


    public Collection<LiveInterval> getNodesHelper(boolean isMove) {
        Collection<Pair<LiveInterval, LiveInterval>> edges;
        if (isMove) {
            edges = getUniqueMoveEdges();
        } else {
            edges = getUniqueInterferenceGraphEdges();
        }

        var allNodes = new HashSet<LiveInterval>();
        for (var edge : edges) {
            allNodes.add(edge.first());
            allNodes.add(edge.second());
        }
        return allNodes;
    }

    public Collection<LiveInterval> getInterferenceGraphNodes() {
        return getNodesHelper(false);
    }

    public Collection<LiveInterval> getMoveNodes() {
        return getNodesHelper(true);
    }

    public Collection<Pair<LiveInterval, LiveInterval>> getUniqueInterferenceGraphEdges() {
        return getUniqueEdgesHelper(graph);
    }

    public Collection<Pair<LiveInterval, LiveInterval>> getUniqueMoveEdges() {
        return getUniqueEdgesHelper(isMoveEdge);
    }
}
