package edu.mit.compilers.asm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class X64Builder {
    private final List<X64Code> x64CodeList;

    public X64Builder() {
        this.x64CodeList = new ArrayList<>();
    }

    public X64Builder addLine(X64Code line) {
        x64CodeList.add(line);
        return this;
    }

    int currentIndex() {
        return x64CodeList.size();
    }

    public X64Code get(int index) {
        return x64CodeList.get(index);
    }

    public X64Builder addAtIndex(int index, X64Code line) {
        x64CodeList.add(index, line);
        return this;
    }

    public X64Builder addLines(Collection<X64Code> lines) {
        x64CodeList.addAll(lines);
        return this;
    }

    public X64Program build() {
        return new X64Program(x64CodeList);
    }

    @Override
    public String toString() {
        return String.join("\n", x64CodeList.stream().map(X64Code::toString).collect(Collectors.joining()));
    }
}
