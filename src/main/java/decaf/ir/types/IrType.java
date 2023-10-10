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
            case Array -> throw new UnsupportedOperationException("Array type does not have a string representation");
        };
    }

    enum TypeID {
        // primitive types
        Int, Bool, String, Void, Pointer,

        // compound types
        Array,

        // undefined type
        Undefined,

        Label,
    }
}
