package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class IrGlobalAddressType extends IrPointerType {
    private static final IrGlobalAddressType globalAddressType = new IrGlobalAddressType();
    protected IrGlobalAddressType() {
        super();
    }

    public static IrGlobalAddressType get() {
        return globalAddressType;
    }

    @Override
    public @NotNull String prettyPrint() {
        return "global ptr";
    }

    @Override
    public int getBitWidth() {
        return 8;
    }
}
