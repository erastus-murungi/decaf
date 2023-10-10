package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

public class IrPointerType extends IrType {
    protected IrPointerType(@NotNull TypeID typeID) {
        super(typeID);
    }
}
