package edu.mit.compilers.codegen.names;

import edu.mit.compilers.utils.Utils;

import java.util.Objects;

public class AbstractName {
    public long size;

    public AbstractName() {
        this.size = Utils.WORD_SIZE;
    }

    public AbstractName(long size) {
        this.size = size;
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

    @Override
    public int hashCode() {
        return Objects.hash(size);
    }
}
