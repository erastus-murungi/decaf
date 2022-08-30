package edu.mit.compilers.dataflow.copy;


import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;

import java.util.Objects;

// a pair (u, v, pos, block), such that u <- v is a copy assignment
// and pos is the position in block is the block where the assignment occurs
// adapted from page 358 of the whale book
public class CopyQuadruple {
    public final LValue u;
    public final Value v;
    public final int position;
    public final BasicBlock basicBlock;

    public CopyQuadruple(LValue u, Value v, int position, BasicBlock basicBlock) {
        this.u = u;
        this.v = v;
        this.position = position;
        this.basicBlock = basicBlock;
    }

    @Override
    public String toString() {
        return String.format("%s ⟵ %s    @ %s in %s", u, v, position, basicBlock.getLeader());
    }

    public boolean contains(Value value) {
        return u.equals(value) || v.equals(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CopyQuadruple that = (CopyQuadruple) o;
        return Objects.equals(u, that.u) && Objects.equals(v, that.v);
    }

    @Override
    public int hashCode() {
        return u.hashCode() ^ v.hashCode();
    }
}
