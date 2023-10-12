package decaf.ir.instructions;

import decaf.ir.types.IrVoidType;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ReturnInstruction extends Instruction {
    // note the return type is either a register or a constant
    @Nullable
    private final IrDirectValue returnValue;

    public ReturnInstruction(@Nullable IrDirectValue irDirectValue) {
        super(irDirectValue == null ? IrVoidType.get() : irDirectValue.getType());
        this.returnValue = irDirectValue;
    }

    public static ReturnInstruction createVoid() {
        return new ReturnInstruction(null);
    }

    public static ReturnInstruction create(@NotNull IrDirectValue irDirectValue) {
        return new ReturnInstruction(irDirectValue);
    }

    @Override
    public String prettyPrint() {
        return String.format("return %s %s",
                             getType().prettyPrint(),
                             (returnValue == null) ? "" : returnValue.prettyPrint()
                            );
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
        return (returnValue == null) ? List.of() : List.of(returnValue);
    }

    public @Nullable Optional<IrDirectValue> getReturnValue() {
        return Optional.ofNullable(returnValue);
    }
}
