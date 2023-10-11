package decaf.ir.values;

import decaf.ir.Counter;
import decaf.ir.types.IrType;
import decaf.shared.CompilationContext;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a register in the IR
 */
public class IrRegister extends IrDirectValue {
    /**
     * The SSA id of this register
     * By default it is zero
     */
    private final int ssaId;

    /**
     * The label id of this register
     * This is allocated by a counter tied to the scope of the function
     */
    private final int labelId;

    protected IrRegister(@NotNull IrType type) {
        super(type);
        this.ssaId = 0;
        this.labelId = Counter.getInstance().nextId();
    }

    public static IrRegister create(@NotNull IrType type) {
        return new IrRegister(type);
    }

    @Override
    public int size() {
        return 8; // assuming 64-bit architecture
    }

    @Override
    public String prettyPrint() {
        return String.format("%%r%d", ssaId);
    }

    @Override
    public String typedPrettyPrint() {
        return String.format("%s %s", getType().prettyPrint(), prettyPrint());
    }

    public int getSsaId() {
        return ssaId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IrRegister other) {
            return this.labelId == other.labelId && this.ssaId == other.ssaId;
        }
        return false;
    }
}
