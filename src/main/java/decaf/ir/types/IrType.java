package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

public class IrType {
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
  private static final IrType Int = new IrType(TypeID.Int);
  private static final IrType Bool = new IrType(TypeID.Bool);
  private static final IrType String = new IrType(TypeID.String);
  private static final IrType Void = new IrType(TypeID.Void);
  private static final IrType Pointer = new IrType(TypeID.Pointer);

  // define singleton instance of undefined type
  public static final IrType Undefined = new IrType(TypeID.Undefined);

  protected IrType(@NotNull TypeID typeID) {
    this.typeID = typeID;
  }

  @NotNull
  public TypeID getTypeID() {
    return typeID;
  }

  public static IrType getIntType() {
    return Int;
  }

  public static IrType getBoolType() {
    return Bool;
  }

  public static IrType getStringType() {
    return String;
  }

  public static IrType getVoidType() {
    return Void;
  }

  public static IrType getPointerType() {
    return Pointer;
  }

  public static IrType getUndefinedType() {
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
