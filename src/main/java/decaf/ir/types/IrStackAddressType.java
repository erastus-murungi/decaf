package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

public class IrStackAddressType extends IrPointerType {
    private static final @NotNull IrStackAddressType irStackAddressType = new IrStackAddressType();
    protected IrStackAddressType() {
        super();
    }

    public static IrStackAddressType get() {
       return irStackAddressType;
    }

    @Override
    public @NotNull String prettyPrint() {
        return "ptr";
    }

    @Override
    public int getBitWidth() {
        return 8;
    }
}
