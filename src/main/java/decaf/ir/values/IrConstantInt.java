package decaf.ir.values;

import decaf.analysis.syntax.ast.types.Type;
import decaf.ir.types.IrIntType;
import decaf.ir.types.IrType;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkState;

public class IrConstantInt extends IrConstant {
    // by default, integers are 32-bit, i.e 4 bytes
    private final int value;

    protected IrConstantInt(int value, int bitWidth) {
        super(IrIntType.createIntN(bitWidth));
        this.value = value;
    }

    public static IrConstantInt create(int value, int numBytes) {
        return new IrConstantInt(value, numBytes);
    }

    public static IrConstantInt create(int value) {
        return new IrConstantInt(value, 4);
    }

    @Override
    public int size() {
        return getType().getBitWidth();
    }

    @Override
    public String prettyPrint() {
        return String.valueOf(value);
    }

    @Override
    public String typedPrettyPrint() {
        return String.format("%s %s", getType().prettyPrint(), prettyPrint());
    }
}
