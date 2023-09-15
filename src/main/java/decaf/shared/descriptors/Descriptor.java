package decaf.shared.descriptors;

import static com.google.common.base.Preconditions.checkNotNull;

import decaf.analysis.syntax.ast.types.ArrayType;
import org.jetbrains.annotations.NotNull;

import decaf.analysis.syntax.ast.types.Type;

/**
 * Every descriptor should at least have a type and an identifier
 */
public class Descriptor {
    @NotNull
    private final Type type;
    private final From from;

    public boolean typeIs(@NotNull Type type) {
        checkNotNull(type);
        return this.type == type;
    }

    public boolean typeIsAnyOf(@NotNull Type... types) {
        checkNotNull(types);
        for (Type type : types) {
            if (typeIs(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean typeIsNotAnyOf(@NotNull Type... types) {
        checkNotNull(types);
        return !typeIsAnyOf(types);
    }

    public @NotNull Type getType() {
        return type;
    }

    public enum From {
        FORMAL_ARGUMENT,
        ARRAY,
        LOCAL_VARIABLE,
        METHOD,

        IS_IMPORT
    }

    protected Descriptor(@NotNull Type type, Descriptor.From from) {
        this.type = type;
        this.from = from;
    }

    public static Descriptor forArray(ArrayType arrayType) {
        return new Descriptor(arrayType, Descriptor.From.ARRAY);
    }

    public static Descriptor forImport() {
        return new Descriptor(Type.getIntType(), Descriptor.From.IS_IMPORT);
    }


    public static Descriptor forFormalArgument(Type type) {
        return new Descriptor(type, Descriptor.From.FORMAL_ARGUMENT);
    }

    public static Descriptor forValue(Type type) {
        return new Descriptor(type, Descriptor.From.LOCAL_VARIABLE);
    }

    public boolean isFormalArgument() {
        return from == Descriptor.From.FORMAL_ARGUMENT;
    }

    public boolean isForArray() {
        return from == Descriptor.From.ARRAY;
    }

    public boolean isMethod() {
        return from == Descriptor.From.METHOD;
    }

    public boolean isImport() {
        return from == From.IS_IMPORT
                ;
    }
}
