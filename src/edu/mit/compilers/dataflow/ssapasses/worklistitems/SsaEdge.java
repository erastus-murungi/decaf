package edu.mit.compilers.dataflow.ssapasses.worklistitems;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.ssa.Phi;


public class SsaEdge {
    /**
     * The instruction which defines this variable
     */
    private final StoreInstruction def;
    private final Instruction use;
    private final BasicBlock useSite;

    public SsaEdge(StoreInstruction def, Instruction use, BasicBlock useSite) {
        this.def = def;
        this.use = use;
        this.useSite = useSite;
    }

    public BasicBlock getUseSite() {
        return useSite;
    }

    public boolean useIsPhi() {
        return use instanceof Phi;
    }

    public StoreInstruction getDef() {
        return def;
    }

    public Instruction getUse() {
        return use;
    }
}
