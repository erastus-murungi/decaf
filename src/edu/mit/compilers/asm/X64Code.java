package edu.mit.compilers.asm;

import java.util.Optional;

public class X64Code {
    String line;
    String comment;

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public X64Code(String line) {
        this.line = line;
    }

    public X64Code(String line, String comment) {
        this.line = line;
        this.comment = comment;
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return line + "        # " + getComment().get();
        return line;
    }
}
