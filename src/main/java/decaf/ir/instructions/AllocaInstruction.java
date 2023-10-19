package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.types.IrStackAddressType;
import decaf.ir.types.IrType;
import decaf.ir.values.IrPointer;
import decaf.ir.values.IrStackPointer;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AllocaInstruction extends Instruction {
    /**
     * Given a type, this alloca instruction gives you a pointer to a memory address that can hold a value of that type.
     * <p>
     * This instruction allocates memory on the stack frame of the currently executing function.
     * The memory is automatically released after the function returns.
     * Thus, you never return memory addresses allocated by this instruction.
     * </p>
     */
    @NotNull
    private final IrStackPointer destination;

    /**
     * The type of the value to be allocated.
     * Note that this type information is not bundled with the destination.
     */
    private final IrType pointeeType;

    /**
     * Create an alloca instruction.
     *
     * @param type The type of the value to be allocated.
     */
    private AllocaInstruction(@NotNull IrType type) {
        super(IrStackAddressType.get());
        this.pointeeType = type;
        this.destination = IrStackPointer.create();
    }

    public static AllocaInstruction create(@NotNull IrType type) {
        return new AllocaInstruction(type);
    }

    @Override
    public String toString() {
        return String.format("%s = alloca %s, align %d", destination.prettyPrint(), pointeeType.prettyPrint(), pointeeType.getBitWidth());
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor,
                                                           ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return List.of(destination);
    }

    public IrType getPointeeType() {
        return pointeeType;
    }

    public IrStackPointer getAddress() {
        return destination;
    }

    public int getNumBytesToAllocate() {
        return pointeeType.getBitWidth();
    }
}
