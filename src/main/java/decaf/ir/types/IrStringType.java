package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class IrStringType extends IrType {
    private static final @NotNull Map<Integer, IrStringType> stringTypeCache = new HashMap<>();
    private final int size;

    protected IrStringType(int size) {
        super();
        this.size = size;
    }

    public static IrStringType get(int size) {
        if (!stringTypeCache.containsKey(size)) {
            stringTypeCache.put(size, new IrStringType(size));
        }
        return stringTypeCache.get(size);
    }

    @Override
    public @NotNull String prettyPrint() {
        return String.format("string<%d>", size);
    }

    @Override
    public int getBitWidth() {
        return size;
    }
}
