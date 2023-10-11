package decaf.ir.instructions;

import decaf.ir.values.IrRegister;
import org.jetbrains.annotations.NotNull;

public interface WithDestination<T extends Instruction> {
    @NotNull IrRegister getDestination();
}
