package edu.mit.compilers.asm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class X64Program extends ArrayList<X64Code> {
    public X64Program(List<X64Code> codes) {
        addAll(codes);
    }

    @Override
    public String toString() {
        return stream().map(X64Code::toString).collect(Collectors.joining("\n"));
    }
}
