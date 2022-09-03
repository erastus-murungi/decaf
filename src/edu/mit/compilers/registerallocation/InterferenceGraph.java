package edu.mit.compilers.registerallocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.utils.GraphVizPrinter;
import edu.mit.compilers.utils.Pair;

public class InterferenceGraph {
    public record Node(LiveInterval liveInterval) {
        @Override
        public String toString() {
            return liveInterval.variable()
                               .toString();
        }

        @Override
        public int hashCode() {
            return liveInterval.hashCode();
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node node)) return false;
            return Objects.equals(liveInterval, node.liveInterval);
        }
    }

    HashMap<Node, Set<Node>> graph = new HashMap<>();
    Map<Node, Set<Node>> isMoveEdge = new HashMap<>();

    public InterferenceGraph(LiveIntervalsUtil liveIntervalsUtil, Method method) {
        var liveIntervals = new ArrayList<>(liveIntervalsUtil.getLiveIntervals(method));
        var varToLivIntervals = liveIntervalsUtil.getValueToLiveIntervalMappingForMethod(method);
        if (liveIntervals.isEmpty()) return;
        for (int i = 0; i < liveIntervals.size(); i++) {
            for (int j = i + 1; j < liveIntervals.size(); j++) {
                var a = new Node(liveIntervals.get(i));
                var b = new Node(liveIntervals.get(j));
                if (LiveIntervalsUtil.liveIntervalsInterfere(a.liveInterval(), b.liveInterval())) {
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
                                              .filter(copyInstruction -> copyInstruction.getValue() instanceof LValue)
                                              .collect(Collectors.toUnmodifiableSet());
        for (var copyInstruction: copyInstructions) {
            var a = new Node(varToLivIntervals.get(copyInstruction.getStore()));
            var b = new Node(varToLivIntervals.get(copyInstruction.getValue()));
            if (!LiveIntervalsUtil.liveIntervalsInterfere(a.liveInterval(), b.liveInterval())) {
                insertMoveEdge(
                        new Node(varToLivIntervals.get(copyInstruction.getStore())),
                        new Node(varToLivIntervals.get(copyInstruction.getValue())));
            }
        }

        GraphVizPrinter.printInterferenceGraph(this, "ig");

    }

    public void insertEdge(Node a, Node b) {
        graph.computeIfAbsent(a, k -> new HashSet<>())
             .add(b);
        graph.computeIfAbsent(b, k -> new HashSet<>())
             .add(a);
    }

    public void insertMoveEdge(Node a, Node b) {
        isMoveEdge.computeIfAbsent(a, k -> new HashSet<>())
                  .add(b);
        isMoveEdge.computeIfAbsent(b, k -> new HashSet<>())
                  .add(a);
    }

    public boolean isMoveEdge(Node a, Node b) {
        if (isMoveEdge.get(a) == null || isMoveEdge.get(b) == null)
            return false;
        return isMoveEdge.get(a)
                         .contains(b) && isMoveEdge.get(b)
                                                   .contains(a);
    }

    public boolean containsEdge(Node a, Node b) {
        return graph.get(a)
                    .contains(b) && graph.get(b)
                                         .contains(a);
    }


    public Collection<Node> getNodesHelper(boolean isMove) {
        Collection<Pair<Node, Node>> edges;
        if (isMove) {
            edges = getUniqueMoveEdges();
        } else {
            edges = getUniqueInterferenceGraphEdges();
        }

        var allNodes = new HashSet<Node>();
        for (var edge: edges) {
            allNodes.add(edge.first());
            allNodes.add(edge.second());
        }
        return allNodes;
    }

    public Collection<Node> getInterferenceGraphNodes() {
        return getNodesHelper(false);
    }

    public Collection<Node> getMoveNodes() {
        return getNodesHelper(true);
    }

    private static Collection<Pair<Node, Node>> getUniqueEdgesHelper(Map<Node, Set<Node>> g) {
        var allEdges = new HashSet<Pair<Node, Node>>();
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

    public Collection<Pair<Node, Node>> getUniqueInterferenceGraphEdges() {
        return getUniqueEdgesHelper(graph);
    }

    public Collection<Pair<Node, Node>> getUniqueMoveEdges() {
        return getUniqueEdgesHelper(isMoveEdge);
    }


}
