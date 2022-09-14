package edu.mit.compilers.ast;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_BOOL;
import static edu.mit.compilers.grammar.DecafScanner.RESERVED_INT;
import static edu.mit.compilers.grammar.DecafScanner.RESERVED_VOID;

import org.jetbrains.annotations.NotNull;

import edu.mit.compilers.utils.Utils;

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
            case Bool -> RESERVED_BOOL;
            case BoolArray -> RESERVED_BOOL + "*";
            case Int -> RESERVED_INT;
            case IntArray -> RESERVED_INT + "*";
            case Void -> RESERVED_VOID;
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