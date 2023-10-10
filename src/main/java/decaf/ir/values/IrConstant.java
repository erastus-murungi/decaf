package decaf.ir.values;

import decaf.ir.types.IrType;
import org.jetbrains.annotations.NotNull;

public abstract class IrConstant extends IrDirectValue {
    public IrConstant(@NotNull IrType type) {
        super(type);
    }
}
