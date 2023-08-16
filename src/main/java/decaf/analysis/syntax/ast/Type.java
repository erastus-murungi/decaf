package decaf.analysis.syntax.ast;


import decaf.analysis.lexical.Scanner;
import decaf.shared.Utils;

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

  public static Type lower(Type type) {
    if (type.equals(IntArray))
      return Int;
    else if (type.equals(BoolArray))
      return Bool;
    return type;
  }

  public String getSourceCode() {
    return switch (this) {
      case Bool -> Scanner.RESERVED_BOOL;
      case BoolArray -> Scanner.RESERVED_BOOL + "*";
      case Int -> Scanner.RESERVED_INT;
      case IntArray -> Scanner.RESERVED_INT + "*";
      case Void -> Scanner.RESERVED_VOID;
      default -> throw new IllegalStateException("Unexpected value: " + this);
    };
  }

  public String getColoredSourceCode() {
    return Utils.coloredPrint(
        getSourceCode(),
        Utils.ANSIColorConstants.ANSI_CYAN
    );
  }

  public long getFieldSize() {
    return switch (this) {
      case Int, IntArray, Bool, BoolArray -> Utils.WORD_SIZE;
      case Void -> 0;
      default -> throw new IllegalStateException("Unexpected value: " + this);
    };
  }
}