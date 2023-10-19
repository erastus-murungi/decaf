package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.types.IrVoidType;
import decaf.ir.values.IrLabel;
import decaf.ir.types.IrType;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnconditionalBranchInstruction extends Instruction {
    @NotNull
    private final IrLabel target;

    protected UnconditionalBranchInstruction(@NotNull IrLabel target) {
        super(IrVoidType.get());
        this.target = target;
    }

    public static UnconditionalBranchInstruction create(@NotNull IrLabel target) {
        return new UnconditionalBranchInstruction(target);
    }

    @Override
    public String toString() {
        return String.format("br %s", target.typedPrettyPrint());
    }
    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor, ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return List.of(target);
    }

    public @NotNull IrLabel getTarget() {
        return target;
    }
}
