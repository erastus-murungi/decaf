package decaf.ir.values;

import decaf.ir.types.IrType;
import org.jetbrains.annotations.NotNull;

public class IrGlobalPointer extends IrPointer {
    /**
     * Names are prefixed with "@"
     * Must have a type
     * Must be initialized
     * // constant pointers
     */
    protected IrGlobalPointer(@NotNull IrType type) {
        super(type);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String prettyPrint() {
        return null;
    }
}
