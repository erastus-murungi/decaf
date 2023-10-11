package decaf.ir.values;

import decaf.ir.types.IrFunctionType;
import decaf.ir.types.IrType;
import org.jetbrains.annotations.NotNull;

public class IrFunctionPointer extends IrValue {
    @NotNull private final String functionName;
    protected IrFunctionPointer(@NotNull String functionName, @NotNull IrFunctionType type) {
        super(type);
        this.functionName = functionName;
    }

    public static IrFunctionPointer create(@NotNull String functionName, @NotNull IrFunctionType type) {
        return new IrFunctionPointer(functionName, type);
    }

    @Override
    public int size() {
        return 8; // TODO: 8 bytes for 64-bit pointers
    }

    @Override
    public String prettyPrint() {
        return String.format("@fn %s", functionName);
    }

    @Override
    public String typedPrettyPrint() {
        return String.format("%s @fn %s", getType().prettyPrint(), functionName);
    }

    public IrType getReturnType() {
        return ((IrFunctionType) getType()).getReturnType();
    }
}
