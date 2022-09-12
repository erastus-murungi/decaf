package edu.mit.compilers.dataflow.ssapasses.worklistitems;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.VirtualRegister;


/**
 * @param def             the {@link Instruction} where a variable where was first defined
 * @param use             a specific use of an {@link Instruction}
 * @param basicBlockOfUse the {@link BasicBlock} where the use is
 */
public record SsaEdge(StoreInstruction def, HasOperand use, BasicBlock basicBlockOfUse) {

    @Override
    public String toString() {
        return String.format("%s in [%s] -> %s", def.getDestination(), def.toString().strip(), use.toString().strip());
    }

    public VirtualRegister getValue() {
        return (VirtualRegister) def.getDestination();
    }
}
