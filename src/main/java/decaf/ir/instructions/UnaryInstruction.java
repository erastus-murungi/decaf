package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnaryInstruction extends Instruction implements WithDestination<UnaryInstruction> {
    @NotNull
    private final IrDirectValue operand;
    @NotNull
    private final UnaryOpType unaryOpType;
    @NotNull
    private final IrRegister destination;

    protected UnaryInstruction(@NotNull IrDirectValue operand,
                               @NotNull IrRegister destination,
                               @NotNull UnaryOpType unaryOpType) {
        super(operand.getType());
        this.operand = operand;
        this.unaryOpType = unaryOpType;
        this.destination = destination;
    }

    public static UnaryInstruction createNot(@NotNull IrDirectValue operand, @NotNull IrRegister destination) {
        return new UnaryInstruction(operand, destination, UnaryOpType.NOT);
    }

    public static UnaryInstruction createNotGenDest(@NotNull IrDirectValue operand) {
        return new UnaryInstruction(operand, IrRegister.create(operand.getType()), UnaryOpType.NOT);
    }

    public static UnaryInstruction createCopy(@NotNull IrDirectValue operand, @NotNull IrRegister destination) {
        return new UnaryInstruction(operand, destination, UnaryOpType.COPY);
    }

    public static UnaryInstruction createCopyGenDest(@NotNull IrDirectValue operand) {
        return new UnaryInstruction(operand, IrRegister.create(operand.getType()), UnaryOpType.COPY);
    }

    @Override
    public String toString() {
        return String.format("%s = %s %s",
                             destination.prettyPrint(),
                             unaryOpType.toString().toLowerCase(),
                             operand.typedPrettyPrint()
                            );
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor,
                                                           ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return List.of(operand, destination);
    }

    @Override
    public @NotNull IrRegister getDestination() {
        return destination;
    }

    public @NotNull IrDirectValue getOperand() {
        return operand;
    }

    public @NotNull UnaryOpType getUnaryOpType() {
        return unaryOpType;
    }

    public enum UnaryOpType {
        NOT, COPY,
    }
}
