package edu.mit.compilers.ast;

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

    public String getSourceCode() {
        return switch (this) {
            case Int, IntArray -> RESERVED_INT;
            case Bool, BoolArray -> RESERVED_BOOL;
            case Void -> RESERVED_VOID;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        };
    }

    public int getFieldSize() {
        return switch (this) {
            case Int, IntArray -> 4;
            case Bool, BoolArray -> 1;
            case Void -> 0;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        };
    }
}