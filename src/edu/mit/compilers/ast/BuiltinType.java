package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Utils;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_INT;
import static edu.mit.compilers.grammar.DecafScanner.RESERVED_BOOL;
import static edu.mit.compilers.grammar.DecafScanner.RESERVED_VOID;

public enum BuiltinType {
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
        switch (this) {
            case Int: case IntArray: case Bool: case BoolArray: return Utils.WORD_SIZE;
            case Void: return 0;
            default: throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}