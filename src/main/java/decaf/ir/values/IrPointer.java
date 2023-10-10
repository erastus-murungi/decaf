package decaf.ir.values;

import decaf.ir.types.IrType;
import org.jetbrains.annotations.NotNull;

/**
 * This is meant to be an opaque pointer without a pointee type
 */
public abstract class IrPointer extends IrValue {
    public IrPointer(@NotNull IrType type) {
        super(IrType.getPointerType());
    }
}
