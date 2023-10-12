package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

public abstract class IrType {
    // define singleton instances of primitive types
    public abstract @NotNull String prettyPrint();

    public abstract int getBitWidth();

    public boolean isIntType() {
        return this instanceof IrIntType;
    }

    public boolean isBoolType() {
        if (this instanceof IrIntType) {
            return this.getBitWidth() == 1;
        }
        return false;
    }
}
