package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ZextInstruction extends Instruction {
    @NotNull private final IrDirectValue src;
    @NotNull private final IrRegister dst;
    public ZextInstruction(@NotNull IrDirectValue src, @NotNull IrRegister dst) {
        super(dst.getType());
        this.src = src;
        this.dst = dst;
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor, ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public String toString() {
        return String.format("%s = zext %s to %s",
                dst.prettyPrint(),
                src.typedPrettyPrint(),
                dst.getType().prettyPrint());
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return null;
    }

    public @NotNull IrDirectValue getSrc() {
        return src;
    }

    public @NotNull IrRegister getDst() {
        return dst;
    }
}
