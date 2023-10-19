package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ZextInstruction extends Instruction {
    @NotNull private final IrDirectValue source;
    @NotNull private final IrRegister destination;
    protected ZextInstruction(@NotNull IrDirectValue source, @NotNull IrRegister destination) {
        super(destination.getType());
        this.source = source;
        this.destination = destination;
    }

    public static ZextInstruction create(@NotNull IrDirectValue source, @NotNull IrRegister destination) {
        return new ZextInstruction(source, destination);
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor, ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public String toString() {
        return String.format("%s = zext %s to %s",
                             destination.prettyPrint(),
                source.typedPrettyPrint(),
                             destination.getType().prettyPrint());
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return null;
    }

    public @NotNull IrDirectValue getSource() {
        return source;
    }

    public @NotNull IrRegister getDestination() {
        return destination;
    }
}
