package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

public class IrType {
    // define singleton instance of undefined type
    public static final IrType Undefined = new IrType(TypeID.Undefined);
    // define singleton instances of primitive types
    private static final IrType Int = new IrType(TypeID.Int);
    private static final IrType Bool = new IrType(TypeID.Bool);
    private static final IrType String = new IrType(TypeID.String);
    private static final IrType Void = new IrType(TypeID.Void);
    private static final IrType LabelType = new IrType(TypeID.Label);

    private static final IrType PointerType = new IrType(TypeID.Pointer);

    @NotNull
    private final TypeID typeID;


    protected IrType(@NotNull TypeID typeID) {
        this.typeID = typeID;
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

    public static IrType getLabelType() {
        return LabelType;
    }

    public static IrType getPointerType() {
        return PointerType;
    }

    public static IrType getUndefinedType() {
        return Undefined;
    }

    @NotNull
    public TypeID getTypeID() {
        return typeID;
    }

    public String getSourceCode() {
        return switch (typeID) {
            case Int -> "int";
            case Bool -> "bool";
            case Void -> "void";
            default -> throw new RuntimeException("source code reverse engineering not implemented for type " + typeID);
        };
    }

    public String prettyPrint() {
        return switch (typeID) {
            // primitive types
            case Int -> "int";
            case Bool -> "bool";
            case String -> "string";
            case Void -> "void";
            case Undefined -> "undefined";
            // the derived types have their own string representation
            // we expect the subclasses to override this method
            case Pointer -> "ptr";
            case Label -> "label";
            case Function -> throw new UnsupportedOperationException("Function type does not have a string representation");
            case Array -> throw new UnsupportedOperationException("Array type does not have a string representation");
        };
    }

    public boolean isBoolType() {
        return this == Bool;
    }

    public boolean isIntType() {
        return this == Int;
    }

    public boolean isStringType() {
        return this == String;
    }

    public boolean isVoidType() {
        return this == Void;
    }

    public boolean isUndefinedType() {
        return this == Undefined;
    }

    public boolean isPointerType() {
        return this == PointerType;
    }

    public boolean isLabelType() {
        return this == LabelType;
    }

    public boolean isFunctionType() {
        return this.getTypeID() == TypeID.Function;
    }

    public boolean isArrayType() {
        return this.getTypeID() == TypeID.Array;
    }

    public boolean isDerivedType() {
        return this.getTypeID() == TypeID.Array || this.getTypeID() == TypeID.Function;
    }

    public boolean isPrimitiveType() {
        return this.getTypeID() == TypeID.Int || this.getTypeID() == TypeID.Bool || this.getTypeID() == TypeID.String || this.getTypeID() == TypeID.Void;
    }

    enum TypeID {
        // primitive types
        Int, Bool, String, Void, Pointer, // all pointers are opaque

        // derived types
        Array, Function,

        // undefined type
        Undefined,

        Label,
    }
}
