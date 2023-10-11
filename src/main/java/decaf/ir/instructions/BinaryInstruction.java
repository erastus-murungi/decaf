package decaf.ir.instructions;

import decaf.ir.types.IrType;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrRegister;
import org.jetbrains.annotations.NotNull;

import decaf.ir.values.IrValue;

import java.util.List;

public class BinaryInstruction extends Instruction implements WithDestination<BinaryInstruction> {
    @NotNull
    private final Op op;
    @NotNull
    private final IrDirectValue lhs;
    @NotNull
    private final IrDirectValue rhs;
    @NotNull
    private final IrRegister destination;

    protected BinaryInstruction(@NotNull Op op,
                                @NotNull IrDirectValue lhs,
                                @NotNull IrDirectValue rhs,
                                @NotNull IrRegister destination) {
        super(lhs.getType());
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
        this.destination = destination;
        checkTypes();
    }

    public static BinaryInstruction createAdd(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.ADD, lhs, rhs, destination);
    }

    public static BinaryInstruction createSub(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.SUB, lhs, rhs, destination);
    }

    public static BinaryInstruction createMul(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.MUL, lhs, rhs, destination);
    }

    public static BinaryInstruction createDiv(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.DIV, lhs, rhs, destination);
    }

    public static BinaryInstruction createMod(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.MOD, lhs, rhs, destination);
    }

    public static BinaryInstruction createShl(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.SHL, lhs, rhs, destination);
    }

    public static BinaryInstruction createShr(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.SHR, lhs, rhs, destination);
    }

    public static BinaryInstruction createAnd(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.AND, lhs, rhs, destination);
    }

    public static BinaryInstruction createOr(@NotNull IrDirectValue lhs,
                                             @NotNull IrDirectValue rhs,
                                             @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.OR, lhs, rhs, destination);
    }

    public static BinaryInstruction createXor(@NotNull IrDirectValue lhs,
                                              @NotNull IrDirectValue rhs,
                                              @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.XOR, lhs, rhs, destination);
    }

    public static BinaryInstruction createEq(@NotNull IrDirectValue lhs,
                                             @NotNull IrDirectValue rhs,
                                             @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.EQ, lhs, rhs, destination);
    }

    public static BinaryInstruction createNe(@NotNull IrDirectValue lhs,
                                             @NotNull IrDirectValue rhs,
                                             @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.NE, lhs, rhs, destination);
    }

    public static BinaryInstruction createLt(@NotNull IrDirectValue lhs,
                                             @NotNull IrDirectValue rhs,
                                             @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.LT, lhs, rhs, destination);
    }

    public static BinaryInstruction createLe(@NotNull IrDirectValue lhs,
                                             @NotNull IrDirectValue rhs,
                                             @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.LE, lhs, rhs, destination);
    }

    public static BinaryInstruction createGt(@NotNull IrDirectValue lhs,
                                             @NotNull IrDirectValue rhs,
                                             @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.GT, lhs, rhs, destination);
    }

    public static BinaryInstruction createGe(@NotNull IrDirectValue lhs,
                                             @NotNull IrDirectValue rhs,
                                             @NotNull IrRegister destination) {
        return new BinaryInstruction(Op.GE, lhs, rhs, destination);
    }

    public static BinaryInstruction createAddGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.ADD, lhs, rhs, IrRegister.create(lhs.getType()));
    }


    public static BinaryInstruction createSubGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.SUB, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createMulGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.MUL, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createDivGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.DIV, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createModGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.MOD, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createShlGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.SHL, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createShrGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.SHR, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createAndGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.AND, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createOrGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.OR, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createXorGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.XOR, lhs, rhs, IrRegister.create(lhs.getType()));
    }

    public static BinaryInstruction createEqGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.EQ, lhs, rhs, IrRegister.create(IrType.getBoolType()));
    }

    public static BinaryInstruction createNeGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.NE, lhs, rhs, IrRegister.create(IrType.getBoolType()));
    }

    public static BinaryInstruction createLtGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.LT, lhs, rhs, IrRegister.create(IrType.getBoolType()));
    }

    public static BinaryInstruction createLeGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.LE, lhs, rhs, IrRegister.create(IrType.getBoolType()));
    }

    public static BinaryInstruction createGtGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.GT, lhs, rhs, IrRegister.create(IrType.getBoolType()));
    }

    public static BinaryInstruction createGeGenDest(@NotNull IrDirectValue lhs, @NotNull IrDirectValue rhs) {
        return new BinaryInstruction(Op.GE, lhs, rhs, IrRegister.create(IrType.getBoolType()));
    }

    private void checkOpSpecificTypes() {
        switch (op) {
            // the ops expect integer types
            case ADD, SUB, MUL, DIV, MOD, SHL, SHR:
                if ( lhs.getType() != IrType.getIntType()) {
                    throw new InstructionMalformed(String.format("lhs type %s is not an integer type",
                                                                 lhs.getType()
                                                                ));
                }
                if (rhs.getType() != IrType.getIntType()) {
                    throw new InstructionMalformed(String.format("rhs type %s is not an integer type",
                                                                 rhs.getType()
                                                                ));
                }
                if (destination.getType() != IrType.getIntType()) {
                    throw new InstructionMalformed(String.format("destination type %s is not an integer type",
                                                                 destination.getType()
                                                                ));
                }
                break;
            case EQ, NE, LT, LE, GE, GT:
                if (lhs.getType() != IrType.getIntType()) {
                    throw new InstructionMalformed(String.format("lhs type %s is not an integer type",
                                                                 lhs.getType()
                                                                ));
                }
                if (lhs.getType() != rhs.getType()) {
                    throw new InstructionMalformed(String.format("lhs type %s is not the same as rhs type %s",
                                                                 lhs.getType(),
                                                                 rhs.getType()
                                                                ));
                }
                if (destination.getType() != IrType.getBoolType()) {
                    throw new InstructionMalformed(String.format("destination type %s is not a boolean type",
                                                                 destination.getType()
                                                                ));
                }
                break;
        }
    }

    private void checkTypes() {
        checkOpSpecificTypes();
    }

    @Override
    public String prettyPrint() {
        return String.format("%s = %s %s %s, %s",
                             destination.prettyPrint(),
                             getOpString(),
                             destination.getType().prettyPrint(),
                             lhs.prettyPrint(),
                             rhs.prettyPrint()
                            );
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    @Override
    public String prettyPrintColored() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return List.of(lhs, rhs, destination);
    }

    public String getOpString() {
        return switch (op) {
            case ADD -> "add";
            case SUB -> "sub";
            case MUL -> "mul";
            case DIV -> "div";
            case MOD -> "mod";
            case SHL -> "shl";
            case SHR -> "shr";
            case AND -> "and";
            case OR -> "or";
            case XOR -> "xor";
            case EQ -> "eq";
            case NE -> "ne";
            case LT -> "lt";
            case LE -> "le";
            case GT -> "gt";
            case GE -> "ge";
        };
    }

    public @NotNull IrRegister getDestination() {
        return destination;
    }


    protected enum Op {
        ADD, SUB, MUL, DIV, MOD, SHL, SHR, AND, OR, XOR, EQ, NE, LT, LE, GT, GE,
    }
}
