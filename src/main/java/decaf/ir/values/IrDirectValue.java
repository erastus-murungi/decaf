package decaf.ir.values;

import decaf.ir.types.IrType;
import org.jetbrains.annotations.NotNull;

public abstract class IrDirectValue extends IrValue {
    public IrDirectValue(@NotNull IrType type) {
        super(type);
    }
}
