package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

public class IrUndefinedType extends IrType {
    private static final @NotNull IrUndefinedType undefinedType = new IrUndefinedType();
    public IrUndefinedType() {
        super();
    }

    @Override
    public @NotNull String prettyPrint() {
        return "undefined";
    }

    public static IrUndefinedType get() {
        return undefinedType;
    }

    @Override
    public int getBitWidth() {
        return 0;
    }

    @Override
    public boolean isFirstClassType() {
        return false;
    }
}
