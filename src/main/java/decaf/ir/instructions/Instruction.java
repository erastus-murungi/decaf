package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.types.IrType;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class Instruction {
    private @NotNull
    final IrType type;

    public Instruction(@NotNull IrType type) {
        this.type = type;
    }

    public abstract String toString();

    public @NotNull IrType getType() {
        return type;
    }

    public abstract List<? extends IrValue> getUsedValues();

    protected abstract <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor,
                                                                 ArgumentType argument);
}
