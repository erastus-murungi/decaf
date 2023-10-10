package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class IrStackAddressType extends IrPointerType {

    /**
     * The type of the pointee. This is an immutable field.
     */
    private final IrType pointeeType;

    private final static Map<IrType, IrStackAddressType> pointerTypesCache = new HashMap<>();

    protected IrStackAddressType(@NotNull IrType pointeeType) {
        super(TypeID.Pointer);
        this.pointeeType = pointeeType;
    }

    public static IrStackAddressType get(@NotNull IrType containedType) {
        if (!pointerTypesCache.containsKey(containedType)) {
            pointerTypesCache.put(containedType, new IrStackAddressType(containedType));
        }
        return pointerTypesCache.get(containedType);
    }

    public IrType getPointeeType() {
        return pointeeType;
    }

}
