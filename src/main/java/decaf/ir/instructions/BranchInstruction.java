package decaf.ir.instructions;

import decaf.ir.types.IrVoidType;
import decaf.ir.values.IrLabel;
import decaf.ir.types.IrType;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class BranchInstruction extends Instruction {
    @NotNull
    private final IrRegister condition;
    @NotNull
    private final IrLabel trueLabel;
    @NotNull
    private final IrLabel falseLabel;

    protected BranchInstruction(@NotNull IrRegister condition,
                                @NotNull IrLabel trueLabel,
                                @NotNull IrLabel falseLabel) {
        super(IrVoidType.get());
        this.condition = condition;
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
    }

    public static BranchInstruction create(@NotNull IrRegister condition,
                                           @NotNull IrLabel trueLabel,
                                           @NotNull IrLabel falseLabel) {
        checkArgument(condition.getType().isBoolType(), "Condition must be a boolean");
        return new BranchInstruction(condition, trueLabel, falseLabel);
    }

    public @NotNull IrRegister getCondition() {
        return condition;
    }

    public @NotNull IrLabel getTrueLabel() {
        return trueLabel;
    }

    public @NotNull IrLabel getFalseLabel() {
        return falseLabel;
    }

    @Override
    public String prettyPrint() {
        return String.format("br %s, %s, %s",
                             condition.typedPrettyPrint(),
                             trueLabel.typedPrettyPrint(),
                             falseLabel.typedPrettyPrint()
                            );
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
        return List.of(condition, trueLabel, falseLabel);
    }

}
