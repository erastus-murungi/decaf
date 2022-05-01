package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.utils.Utils;

import java.util.Objects;

public abstract class AbstractName {
    public long size;
    public BuiltinType builtinType;
    public final String value;

    public AbstractName(BuiltinType builtinType, String value) {
        this(Utils.WORD_SIZE, builtinType, value);
    }

    public AbstractName(long size, BuiltinType builtinType, String value) {
        this.builtinType = builtinType;
        this.size = size;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractName that = (AbstractName) o;
        return toString().equals(that.toString());
    }

    public abstract String repr();
}
