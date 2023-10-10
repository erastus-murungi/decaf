package decaf.ir.values;

import decaf.analysis.syntax.ast.types.Type;
import decaf.ir.types.IrType;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkState;

public class IrConstantInt extends IrConstant {
    // by default, integers are 32-bit, i.e 4 bytes
    private final int numBytes;
    private final int value;

    protected IrConstantInt(int value, int numBytes) {
        super(IrType.getIntType());
        checkState(numBytes == 1 || numBytes == 2 || numBytes == 4 || numBytes == 8,
                   "Invalid number of bytes for integer constant; must be 1, 2, 4, or 8"
                  );
        this.numBytes = numBytes;
        this.value = value;
    }

    public static IrConstantInt create(int value, int numBytes) {
        return new IrConstantInt(value, numBytes);
    }

    public static IrConstantInt create(int value) {
        return new IrConstantInt(value, 4);
    }

    public int getValue() {
        return value;
    }

    public int getNumBytes() {
        return numBytes;
    }

    @Override
    public int size() {
        return numBytes;
    }

    @Override
    public String prettyPrint() {
        return String.valueOf(value);
    }

    @Override
    public @NotNull IrType getType() {
        return IrType.getIntType();
    }
}
