package decaf.ir.instructions;

import decaf.ir.types.IrType;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ZextInstruction extends Instruction {
    public ZextInstruction(@NotNull IrType type) {
        super(type);
    }

    @Override
    public String prettyPrint() {
        return null;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public String prettyPrintColored() {
        return null;
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return null;
    }
}
