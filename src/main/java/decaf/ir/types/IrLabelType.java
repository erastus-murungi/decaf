package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

public class IrLabelType extends IrType {
    private static final @NotNull IrLabelType labelType = new IrLabelType();

    protected IrLabelType() {
        super();
    }

    public static IrLabelType get() {
        return labelType;
    }

    @Override
    public @NotNull String prettyPrint() {
        return "label";
    }

    @Override
    public int getBitWidth() {
        return 0;
    }
}
