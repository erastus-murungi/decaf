package edu.mit.compilers.asm;

import java.util.Objects;

public class X64Code {
    String line;
    String comment;

    public X64Code(String line) {
        this.line = line;
    }

    public X64Code(String line, String comment) {
        this.line = line;
        this.comment = comment;
    }

    @Override
    public String toString() {
        if (comment != null) {
            return String.format("%s%s # %s", line, "\t", comment);
        }
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        X64Code x64Code = (X64Code) o;
        return Objects.equals(line.strip(), x64Code.line.strip());
    }

    @Override
    public int hashCode() {
        return Objects.hash(line.strip());
    }
}
