package edu.mit.compilers.dataflow.ssapasses.worklistitems;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.ssa.Phi;


/**
 *
 * @param def the {@link Instruction} where a variable where was first defined
 * @param use a specific use of an {@link Instruction}
 * @param useSite the {@link BasicBlock} where the use is
 */
public record SsaEdge(StoreInstruction def, Instruction use, BasicBlock useSite) {

    public boolean useIsPhi() {
        return use instanceof Phi;
    }
}
