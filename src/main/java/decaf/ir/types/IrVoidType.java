package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

/**
 * The void type does not represent any value and has no size.
 */
public class IrVoidType extends IrPrimitiveType {
    private static final IrVoidType voidType = new IrVoidType();
    protected IrVoidType() {
        super();
    }
    public static IrVoidType get() {
        return voidType;
    }

    @Override
    public @NotNull String prettyPrint() {
        return "void";
    }

    @Override
    public int getBitWidth() {
        return 0;
    }
}
