package decaf.dataflow.ssapasses.worklistitems;

import decaf.codegen.codes.HasOperand;
import decaf.cfg.BasicBlock;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.names.IrRegister;


/**
 * @param def             the {@link Instruction} where a irAssignableValue where was first defined
 * @param use             a specific use of an {@link Instruction}
 * @param basicBlockOfUse the {@link BasicBlock} where the use is
 */
public record SsaEdge(StoreInstruction def, HasOperand use, BasicBlock basicBlockOfUse) {

    @Override
    public String toString() {
        return String.format("%s in [%s] -> %s", def.getDestination(), def.toString().strip(), use.toString().strip());
    }

    public IrRegister getValue() {
        return (IrRegister) def.getDestination();
    }
}
