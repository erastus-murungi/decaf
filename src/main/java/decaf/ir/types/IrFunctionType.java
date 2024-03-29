package decaf.ir.types;

import decaf.ir.values.IrGlobalPointer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class IrFunctionType extends IrDerivedType {
    private @NotNull
    final IrType returnType;

    private @NotNull
    final IrType[] paramTypes;

    protected IrFunctionType(@NotNull IrType returnType, @NotNull IrType[] paramTypes) {
        super();
        this.returnType = returnType;
        this.paramTypes = paramTypes;
        // check that the return type is a first class type or void
        checkArgument(returnType.isFirstClassType() || returnType == IrVoidType.get(),
                      "return type must be a first class type or void");
    }

    public static IrFunctionType create(@NotNull IrType returnType, @NotNull IrType[] paramTypes) {
        return new IrFunctionType(returnType, paramTypes);
    }

    public @NotNull IrType getReturnType() {
        return returnType;
    }

    public @NotNull IrType[] getParamTypes() {
        return paramTypes;
    }

    public @NotNull IrType getParamType(int index) {
        return paramTypes[index];
    }

    public int getNumParams() {
        return paramTypes.length;
    }

    public @NotNull String prettyPrint() {
        return String.format("(%s) -> %s",
                             Arrays.stream(paramTypes).map(IrType::prettyPrint).collect(Collectors.joining(", ")),
                             returnType.prettyPrint()
                             );
    }

    @Override
    public int getBitWidth() {
        return 8;
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IrFunctionType that = (IrFunctionType) o;
        return Objects.equals(returnType, that.returnType) && Arrays.equals(paramTypes, that.paramTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(returnType);
        result = 31 * result + Arrays.hashCode(paramTypes);
        return result;
    }
}
