package decaf.ir.instructions;

import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnaryInstruction extends Instruction implements WithDestination<UnaryInstruction> {
    @NotNull
    private final IrDirectValue operand;
    @NotNull
    private final Op op;
    @NotNull
    private final IrRegister destination;

    protected UnaryInstruction(@NotNull IrDirectValue operand, @NotNull IrRegister destination, @NotNull Op op) {
        super(operand.getType());
        this.operand = operand;
        this.op = op;
        this.destination = destination;
    }

    public static UnaryInstruction createNeg(@NotNull IrDirectValue operand, @NotNull IrRegister destination) {
        return new UnaryInstruction(operand, destination, Op.NOT);
    }

    public static UnaryInstruction createNegGenDest(@NotNull IrDirectValue operand) {
        return new UnaryInstruction(operand, IrRegister.create(operand.getType()), Op.NOT);
    }

    public static UnaryInstruction createNot(@NotNull IrDirectValue operand, @NotNull IrRegister destination) {
        return new UnaryInstruction(operand, destination, Op.NOT);
    }

    public static UnaryInstruction createNotGenDest(@NotNull IrDirectValue operand) {
        return new UnaryInstruction(operand, IrRegister.create(operand.getType()), Op.NOT);
    }

    public static UnaryInstruction createCopy(@NotNull IrDirectValue operand, @NotNull IrRegister destination) {
        return new UnaryInstruction(operand, destination, Op.COPY);
    }

    public static UnaryInstruction createCopyGenDest(@NotNull IrDirectValue operand) {
        return new UnaryInstruction(operand, IrRegister.create(operand.getType()), Op.COPY);
    }

    @Override
    public String prettyPrint() {
        return String.format("%s = %s %s",
                             destination.prettyPrint(),
                             op.toString().toLowerCase(),
                             operand.typedPrettyPrint()
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
        return List.of(operand, destination);
    }

    @Override
    public @NotNull IrRegister getDestination() {
        return destination;
    }

    enum Op {
        NEGATE, NOT, COPY,
    }
}
