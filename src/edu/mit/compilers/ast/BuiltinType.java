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
        switch (this) {
            case Int: case IntArray: return RESERVED_INT;
            case Bool: case BoolArray: return RESERVED_BOOL;
            case Void: return RESERVED_VOID;
            default: throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public int getFieldSize() {
        switch (this) {
            case Int: case IntArray: return 8;
            case Bool: case BoolArray: return 1;
            case Void: return 0;
            default: throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}