package decaf.ast;

import org.jetbrains.annotations.NotNull;

import decaf.common.Utils;
import decaf.grammar.DecafScanner;

public enum Type {
    Int,
    Void,
    Bool,
    IntArray,
    BoolArray,
    String,
    Undefined;

//    public String getSourceCode() {
//        return switch (this) {
//            case Int, IntArray -> RESERVED_INT;
//            case Bool, BoolArray -> RESERVED_BOOL;
//            case Void -> RESERVED_VOID;
//            default -> throw new IllegalStateException("Unexpected value: " + this);
//        };
//    }

    public static Type lower(@NotNull Type type) {
        if (type.equals(IntArray))
            return Int;
        else if (type.equals(BoolArray))
            return Bool;
        return type;
    }

    public String getSourceCode() {
        return switch (this) {
            case Bool -> DecafScanner.RESERVED_BOOL;
            case BoolArray -> DecafScanner.RESERVED_BOOL + "*";
            case Int -> DecafScanner.RESERVED_INT;
            case IntArray -> DecafScanner.RESERVED_INT + "*";
            case Void -> DecafScanner.RESERVED_VOID;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        };
    }

    public String getColoredSourceCode() {
        return Utils.coloredPrint(getSourceCode(), Utils.ANSIColorConstants.ANSI_CYAN);
    }

    public long getFieldSize() {
        return switch (this) {
            case Int, IntArray, Bool, BoolArray -> Utils.WORD_SIZE;
            case Void -> 0;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        };
    }
}