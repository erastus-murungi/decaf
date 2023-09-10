package decaf.shared.types;

import org.jetbrains.annotations.NotNull;

public class Type {
  public enum TypeID {
    // primitive types
    Int,
    Bool,
    String,
    Void,
    Pointer,

    // compound types
    Array,

    // undefined type
    Undefined,
  }

  @NotNull
  private final TypeID typeID;

  // define singleton instances of primitive types
  private static final Type Int = new Type(TypeID.Int);
  private static final Type Bool = new Type(TypeID.Bool);
  private static final Type String = new Type(TypeID.String);
  private static final Type Void = new Type(TypeID.Void);
  private static final Type Pointer = new Type(TypeID.Pointer);

  // define singleton instance of undefined type
  public static final Type Undefined = new Type(TypeID.Undefined);

  protected Type(@NotNull TypeID typeID) {
    this.typeID = typeID;
  }

  @NotNull
  public TypeID getTypeID() {
    return typeID;
  }

  public static Type getIntType() {
    return Int;
  }

  public static Type getBoolType() {
    return Bool;
  }

  public static Type getStringType() {
    return String;
  }

  public static Type getVoidType() {
    return Void;
  }

  public static Type getPointerType() {
    return Pointer;
  }

  public static Type getUndefinedType() {
    return Undefined;
  }

  public String getSourceCode() {
    return switch (typeID) {
      case Int -> "int";
      case Bool -> "bool";
      case Void -> "void";
      default -> throw new RuntimeException("source code reverse engineering not implemented for type " + typeID);
    };
  }
}
