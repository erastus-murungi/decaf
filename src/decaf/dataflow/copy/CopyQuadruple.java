package decaf.dataflow.copy;


import java.util.Objects;

import decaf.cfg.BasicBlock;
import decaf.codegen.names.IrValue;
import decaf.codegen.names.IrSsaRegister;

// a pair (u, v, pos, block), such that u <- v is a copy assignment
// and pos is the position in block is the block where the assignment occurs
// adapted from page 358 of the whale book
public record CopyQuadruple(IrSsaRegister u, IrValue v, int position, BasicBlock basicBlock) {

    @Override
    public String toString() {
        return String.format("%s ⟵ %s    @ %s in %s", u, v, position, basicBlock.getLeader());
    }

    public boolean contains(IrValue irValue) {
        return u.equals(irValue) || v.equals(irValue);
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
