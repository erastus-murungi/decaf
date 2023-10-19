package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

public class IrIntType extends IrPrimitiveType {
    @Override
    public @NotNull String prettyPrint() {
        return toString();
    }

    protected enum IntType {
        i1, i2, i4, i8, i16, i32, i64
    }


    @NotNull private final IntType intType;

    private static final IrIntType i1 = new IrIntType(IntType.i1);
    private static final IrIntType i2 = new IrIntType(IntType.i2);
    private static final IrIntType i4 = new IrIntType(IntType.i4);
    private static final IrIntType i8 = new IrIntType(IntType.i8);
    private static final IrIntType i16 = new IrIntType(IntType.i16);
    private static final IrIntType i32 = new IrIntType(IntType.i32);
    private static final IrIntType i64 = new IrIntType(IntType.i64);

    protected IrIntType(@NotNull IntType intType) {
        super();
        this.intType = intType;
    }

    @Override
    public String toString() {
        return switch (intType) {
            case i1 -> "i1";
            case i2 -> "i2";
            case i4 -> "i4";
            case i8 -> "i8";
            case i16 -> "i16";
            case i32 -> "i32";
            case i64 -> "i64";
        };
    }

    public static IrIntType getInt1() {
        return i1;
    }

    public static IrIntType createInt2() {
        return i2;
    }

    public static IrIntType createInt4() {
        return i4;
    }

    public static IrIntType createInt8() {
        return i8;
    }

    public static IrIntType createInt16() {
        return i16;
    }

    public static IrIntType createInt32() {
        return i32;
    }

    public static IrIntType getInt64() {
        return i64;
    }

    public static IrIntType getDefaultInt() {
        return getInt64();
    }

    public static IrIntType getBoolType() {
        return getInt1();
    }

    public static IrIntType createIntN(int n) {
        return switch (n) {
            case 1 -> i1;
            case 2 -> i2;
            case 4 -> i4;
            case 8 -> i8;
            case 16 -> i16;
            case 32 -> i32;
            case 64 -> i64;
            default -> throw new IllegalArgumentException("Invalid integer size");
        };
    }

    public int getBitWidth() {
        return switch (intType) {
            case i1 -> 1;
            case i2 -> 2;
            case i4 -> 4;
            case i8 -> 8;
            case i16 -> 16;
            case i32 -> 32;
            case i64 -> 64;
        };
    }

    @Override
    public boolean isFirstClassType() {
        return true;
    }
}
