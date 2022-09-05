package edu.mit.compilers.ast;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_BOOL;
import static edu.mit.compilers.grammar.DecafScanner.RESERVED_INT;
import static edu.mit.compilers.grammar.DecafScanner.RESERVED_VOID;

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

    public String getSourceCode() {
        switch (this) {
            case Bool:
                return RESERVED_BOOL;
            case BoolArray:
                return RESERVED_BOOL + "*";
            case Int:
                return RESERVED_INT;
            case IntArray:
                return RESERVED_INT + "*";
            case Void:
                return RESERVED_VOID;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public String getColoredSourceCode() {
        return Utils.coloredPrint(getSourceCode(), Utils.ANSIColorConstants.ANSI_CYAN);
    }

    public long getFieldSize() {
        switch (this) {
            case Int:
            case IntArray:
            case Bool:
            case BoolArray:
                return Utils.WORD_SIZE;
            case Void:
                return 0;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}