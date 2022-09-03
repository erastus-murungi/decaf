package edu.mit.compilers.dataflow.ssapasses.worklistitems;

import java.util.Objects;

import edu.mit.compilers.cfg.BasicBlock;

public record FlowEdge(BasicBlock start, BasicBlock sink) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowEdge edge = (FlowEdge) o;
        return Objects.equals(start(), edge.start()) && Objects.equals(sink(), edge.sink());
    }

    @Override
    public int hashCode() {
        return Objects.hash(start(), sink());
    }
}
