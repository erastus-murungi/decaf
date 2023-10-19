package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.types.IrType;
import decaf.ir.types.IrUndefinedType;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrPointer;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The ‘store’ instruction is used to write to memory. The first operand is the value to be written, the second is the
 * address to write to. The type of the value must match the type of the pointer.
 */
public class StoreInstruction extends Instruction {
    @NotNull
    private final IrDirectValue value;
    @NotNull
    private final IrPointer irPointer;

    protected StoreInstruction(@NotNull IrDirectValue value, @NotNull IrPointer irPointer) {
        super(value.getType());
        this.value = value;
        this.irPointer = irPointer;
    }

    public static StoreInstruction create(@NotNull IrDirectValue irDirectValue, @NotNull IrPointer irPointer) {
        return new StoreInstruction(irDirectValue, irPointer);
    }

    @Override
    public String toString() {
        return String.format("store %s, %s", value.typedPrettyPrint(), irPointer.typedPrettyPrint());
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor, ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return List.of(value, irPointer);
    }

    public @NotNull IrDirectValue getValue() {
        return value;
    }

    public @NotNull IrPointer getAddress() {
        return irPointer;
    }
}
