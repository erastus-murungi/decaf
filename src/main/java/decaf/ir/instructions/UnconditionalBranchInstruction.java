package decaf.ir.instructions;

import decaf.ir.types.IrLabel;
import decaf.ir.types.IrType;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnconditionalBranchInstruction extends Instruction {
    @NotNull
    private final IrLabel target;

    protected UnconditionalBranchInstruction(@NotNull IrLabel target) {
        super(IrType.getVoidType());
        this.target = target;
    }

    public static UnconditionalBranchInstruction create(@NotNull IrLabel target) {
        return new UnconditionalBranchInstruction(target);
    }

    @Override
    public String prettyPrint() {
        return String.format("br %s", target.typedPrettyPrint());
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    @Override
    public String prettyPrintColored() {
        return null;
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return List.of(target);
    }

    public @NotNull IrLabel getTarget() {
        return target;
    }
}
