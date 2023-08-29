package decaf.ir.dataflow.ssapasses.worklistitems;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.names.IrSsaRegister;


/**
 * @param def             the {@link Instruction} where a irAssignableValue where was first defined
 * @param use             a specific use of an {@link Instruction}
 * @param basicBlockOfUse the {@link BasicBlock} where the use is
 */
public record SsaEdge(StoreInstruction def, HasOperand use, BasicBlock basicBlockOfUse) {

  @Override
  public String toString() {
    return String.format(
        "%s in [%s] -> %s",
        def.getDestination(),
        def.toString()
           .strip(),
        use.toString()
           .strip()
    );
  }

  public IrSsaRegister getValue() {
    return (IrSsaRegister) def.getDestination();
  }
}
