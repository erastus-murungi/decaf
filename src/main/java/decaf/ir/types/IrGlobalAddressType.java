package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class IrGlobalAddressType extends IrPointerType {
    private final @NotNull IrType containedType;

    private final static Map<IrType, IrGlobalAddressType> pointerTypesCache = new HashMap<>();
    protected IrGlobalAddressType(@NotNull IrType containedType) {
        super(TypeID.Pointer);
        this.containedType = containedType;
    }

    public static IrGlobalAddressType get(@NotNull IrType containedType) {
        if (!pointerTypesCache.containsKey(containedType)) {
            pointerTypesCache.put(containedType, new IrGlobalAddressType(containedType));
        }
        return pointerTypesCache.get(containedType);
    }

    public @NotNull IrType getContainedType() {
        return containedType;
    }
}
