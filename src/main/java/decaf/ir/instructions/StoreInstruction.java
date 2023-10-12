package decaf.ir.instructions;

import decaf.ir.types.IrType;
import decaf.ir.types.IrUndefinedType;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrPointer;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StoreInstruction extends Instruction {
    @NotNull
    private final IrDirectValue irDirectValue;
    @NotNull
    private final IrPointer irPointer;

    protected StoreInstruction(@NotNull IrDirectValue irDirectValue, @NotNull IrPointer irPointer) {
        super(IrUndefinedType.get());
        this.irDirectValue = irDirectValue;
        this.irPointer = irPointer;
    }

    public static StoreInstruction create(@NotNull IrDirectValue irDirectValue, @NotNull IrPointer irPointer) {
        return new StoreInstruction(irDirectValue, irPointer);
    }

    @Override
    public String prettyPrint() {
        return String.format("store %s, %s", irDirectValue.typedPrettyPrint(), irPointer.typedPrettyPrint());
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    @Override
    public String prettyPrintColored() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return List.of(irDirectValue, irPointer);
    }
}
