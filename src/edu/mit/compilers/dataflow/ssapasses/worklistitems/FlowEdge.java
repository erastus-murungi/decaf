package edu.mit.compilers.dataflow.ssapasses.worklistitems;

import java.util.Objects;

import edu.mit.compilers.cfg.BasicBlock;

public class FlowEdge {
    private final BasicBlock start;
    private final BasicBlock sink;


    public FlowEdge(BasicBlock start, BasicBlock sink) {
        this.start = start;
        this.sink = sink;
    }

    public BasicBlock getSink() {
        return sink;
    }

    public BasicBlock getStart() {
        return start;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowEdge edge = (FlowEdge) o;
        return Objects.equals(getStart(), edge.getStart()) && Objects.equals(getSink(), edge.getSink());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getSink());
    }
}
