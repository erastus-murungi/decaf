package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.types.IrIntType;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompareInstruction extends Instruction {
    protected final IrDirectValue lhs;
    protected final IrDirectValue rhs;
    protected final IrRegister destination;
    protected final CompareType compareType;
    protected CompareInstruction(@NotNull CompareType compareType,
                                 @NotNull IrDirectValue lhs,
                                 @NotNull IrDirectValue rhs,
                                 @NotNull IrRegister destination) {
        super(IrIntType.getBoolType());
        this.compareType = compareType;
        this.lhs = lhs;
        this.rhs = rhs;
        this.destination = destination;
    }

    public static CompareInstruction createEqGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new CompareInstruction(CompareType.EQ, lhs, rhs, IrRegister.create(IrIntType.getInt1()));
    }

    public static CompareInstruction createNeGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new CompareInstruction(CompareType.NE, lhs, rhs, IrRegister.create(IrIntType.getInt1()));
    }

    public static CompareInstruction createLtGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new CompareInstruction(CompareType.SLT, lhs, rhs, IrRegister.create(IrIntType.getInt1()));
    }

    public static CompareInstruction createLeGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new CompareInstruction(CompareType.SLE, lhs, rhs, IrRegister.create(IrIntType.getInt1()));
    }

    public static CompareInstruction createGtGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new CompareInstruction(CompareType.SGT, lhs, rhs, IrRegister.create(IrIntType.getInt1()));
    }

    public static CompareInstruction createGeGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new CompareInstruction(CompareType.SGE, lhs, rhs, IrRegister.create(IrIntType.getInt1()));
    }

    public static CompareInstruction createEq(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new CompareInstruction(CompareType.EQ, lhs, rhs, destination);
    }

    public static CompareInstruction createNe(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new CompareInstruction(CompareType.NE, lhs, rhs, destination);
    }

    public static CompareInstruction createLt(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new CompareInstruction(CompareType.SLT, lhs, rhs, destination);
    }

    public static CompareInstruction createLe(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new CompareInstruction(CompareType.SLE, lhs, rhs, destination);
    }

    public static CompareInstruction createGt(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new CompareInstruction(CompareType.SGT, lhs, rhs, destination);
    }

    public static CompareInstruction createGe(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new CompareInstruction(CompareType.SGE, lhs, rhs, destination);
    }

    @Override
    public String toString() {
        return String.format("%s = %s %s %s",
                destination.prettyPrint(),
                getOpString(),
                lhs.typedPrettyPrint(),
                rhs.typedPrettyPrint());
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return null;
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor, ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    public String getOpString() {
        return switch (compareType) {
            case EQ -> "eq";
            case NE -> "ne";
            case SGT -> "sgt";
            case SGE -> "sge";
            case SLT -> "slt";
            case SLE -> "sle";
        };
    }

    protected enum CompareType {
        EQ, NE, SGT, SGE, SLT, SLE,
    }

    public IrDirectValue getRhs() {
        return rhs;
    }

    public IrDirectValue getLhs() {
        return lhs;
    }

    public IrRegister getDestination() {
        return destination;
    }
}
