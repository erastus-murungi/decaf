package decaf.ir.types;

import decaf.ir.Counter;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

public class IrLabel extends IrValue {
    private final @NotNull String id;

    protected IrLabel(@NotNull String id) {
        super(IrType.getLabelType());
        this.id = id;
    }

    public static IrLabel createNamed(@NotNull String id) {
        return new IrLabel(id);
    }

    public static IrLabel create() {
        return new IrLabel(String.format("L%s", Counter.getInstance().nextId()));
    }

    @Override
    public int size() {
        return 8; // TODO: This is a guess
    }

    @Override
    public String prettyPrint() {
        return id;
    }

    @Override
    public String typedPrettyPrint() {
        return String.format("label %s", prettyPrint());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IrLabel)) {
            return false;
        }
        return id.equals(((IrLabel) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
