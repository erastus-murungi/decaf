package edu.mit.compilers.asm;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class X64Program implements Iterable<X64Code> {
    List<X64Code> codes;

    public X64Program(List<X64Code> codes) {
        this.codes = codes;
    }

    @Override
    public String toString() {
        return codes.stream().map(X64Code::toString).collect(Collectors.joining("\n"));
    }

    @Override
    public Iterator<X64Code> iterator() {
        return codes.iterator();
    }

    @Override
    public void forEach(Consumer<? super X64Code> action) {
        codes.forEach(action);
    }

    @Override
    public Spliterator<X64Code> spliterator() {
        return codes.spliterator();
    }
}
