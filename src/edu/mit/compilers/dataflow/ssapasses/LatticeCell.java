package edu.mit.compilers.dataflow.ssapasses;

import edu.mit.compilers.codegen.names.Value;

class LatticeCell {
    Value v;
    LatticeElement latticeElement;

    public LatticeCell(Value v, LatticeElement latticeElement) {
        this.v = v;
        this.latticeElement = latticeElement;
    }

    @Override
    public String toString() {
        return String.format("%s ⟶ %s", v, latticeElement);
    }
}
