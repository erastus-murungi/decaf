package decaf.ir.values;

import decaf.ir.instructions.Instruction;
import decaf.ir.types.IrType;
import decaf.shared.LinkedListSet;
import org.jetbrains.annotations.NotNull;

public abstract class IrValue {
    /**
     * The instructions that use this value.
     * So far we are only considering instructions that use this value
     */
    private LinkedListSet<Instruction> users;
    /**
     * Get the size of this value in bytes.
     */

    private final @NotNull IrType type;

    public IrValue(@NotNull IrType type) {
        this.type = type;
        this.users = new LinkedListSet<>();
    }

    public abstract int size();

    public abstract String prettyPrint();

    public abstract String typedPrettyPrint();

    public @NotNull IrType getType() {
        return type;
    }
}
