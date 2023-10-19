package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.types.IrIntType;
import decaf.ir.types.IrVoidType;
import decaf.ir.values.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

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

    public static BinaryInstruction negate(@NotNull IrDirectValue irDirectValue, @NotNull IrRegister destination) {
        checkArgument(destination.getType() instanceof IrIntType);
        final var intType = (IrIntType) destination.getType();
        return new BinaryInstruction(BinaryInstruction.BinaryOperatorType.SUB,
                                     IrConstantInt.create(0, intType.getBitWidth()),
                                     irDirectValue,
                                     destination
        );
    }

    public static BinaryInstruction negateGenDest(@NotNull IrDirectValue irDirectValue) {
        checkArgument(irDirectValue.getType() instanceof IrIntType);
        final var intType = (IrIntType) irDirectValue.getType();
        return new BinaryInstruction(BinaryInstruction.BinaryOperatorType.SUB,
                                     IrConstantInt.create(0, intType.getBitWidth()),
                                     irDirectValue,
                                     IrRegister.create(intType)
        );
    }

    public @NotNull IrRegister getCondition() {
        return condition;
    }

    public @NotNull IrLabel getTrueTarget() {
        return trueLabel;
    }

    public @NotNull IrLabel getFalseTarget() {
        return falseLabel;
    }

    @Override
    public String toString() {
        return String.format("br %s, %s, %s",
                             condition.typedPrettyPrint(),
                             trueLabel.typedPrettyPrint(),
                             falseLabel.typedPrettyPrint()
                            );
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor,
                                                           ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return List.of(condition, trueLabel, falseLabel);
    }
}
