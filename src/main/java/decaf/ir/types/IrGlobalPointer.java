package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class IrGlobalPointer extends IrPointerType {
    private final @NotNull IrType containedType;

    private final static Map<IrType, IrGlobalPointer> pointerTypesCache = new HashMap<>();
    protected IrGlobalPointer(@NotNull IrType containedType) {
        super(TypeID.Pointer);
        this.containedType = containedType;
    }

    public static IrGlobalPointer get(@NotNull IrType containedType) {
        if (!pointerTypesCache.containsKey(containedType)) {
            pointerTypesCache.put(containedType, new IrGlobalPointer(containedType));
        }
        return pointerTypesCache.get(containedType);
    }

    public @NotNull IrType getContainedType() {
        return containedType;
    }
}
