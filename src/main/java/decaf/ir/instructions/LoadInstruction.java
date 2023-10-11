package decaf.ir.instructions;

import decaf.ir.types.IrType;
import decaf.ir.values.IrPointer;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LoadInstruction extends Instruction {
    @NotNull
    final IrRegister destination;
    @NotNull
    private final IrPointer memoryAddress;

    protected LoadInstruction(@NotNull IrPointer memoryAddress, @NotNull IrRegister destination) {
        super(destination.getType());
        this.memoryAddress = memoryAddress;
        this.destination = destination;
    }

    public static LoadInstruction withNamedDestination(@NotNull IrPointer memoryAddress, @NotNull IrRegister destination) {
        return new LoadInstruction(memoryAddress, destination);
    }

    public static LoadInstruction create(@NotNull IrPointer memoryAddress, @NotNull IrType type) {
        return new LoadInstruction(memoryAddress, IrRegister.create(type));
    }

    @Override
    public String prettyPrint() {
        return String.format("%s = load %s, %s",
                             destination.prettyPrint(),
                             destination.getType().prettyPrint(),
                             memoryAddress.typedPrettyPrint()
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
        return List.of(memoryAddress, destination);
    }

    public IrPointer getMemoryAddress() {
        return memoryAddress;
    }
}
