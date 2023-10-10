package decaf.ir.values;

import decaf.ir.Counter;
import decaf.ir.types.IrStackAddressType;
import decaf.ir.types.IrType;
import org.jetbrains.annotations.NotNull;

public class IrStackPointer extends IrPointer {
    @NotNull private String identifier;
    protected IrStackPointer(@NotNull String identifier) {
        super(IrType.getPointerType());
        this.identifier = identifier;
    }

    public static IrStackPointer create() {
        return new IrStackPointer(String.valueOf(Counter.getInstance().nextId()));
    }

    public static IrStackPointer createNamed(@NotNull String name) {
        return new IrStackPointer(name);
    }

    @Override
    public int size() {
        return 8; // assuming 64-bit architecture
    }

    @Override
    public String prettyPrint() {
        return String.format("*%s", identifier);
    }
}
