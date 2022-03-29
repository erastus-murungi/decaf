package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.*;

import java.util.*;
import java.util.function.Consumer;

public class ThreeAddressCodeList implements Iterable<ThreeAddressCode> {
    public AbstractName place;
    private final List<ThreeAddressCode> codes;
    public static final AbstractName UNDEFINED = new VariableName(null, -1);

    private ThreeAddressCodeList next;
    private ThreeAddressCodeList prev;

    public Optional<ThreeAddressCodeList> getNext() {
        return Optional.of(next);
    }

    public void setNext(ThreeAddressCodeList next) {
        this.next = next;
    }

    public Optional<ThreeAddressCodeList> getPrev() {
        return Optional.of(prev);
    }

    public void setPrev(ThreeAddressCodeList prev) {
        this.prev = prev;
    }

    public ThreeAddressCodeList(AbstractName place, List<ThreeAddressCode> codes) {
        this.place = place;
        this.codes = codes;
    }

    public ThreeAddressCodeList(AbstractName place) {
        codes = new ArrayList<>();
        if (place == null)
            throw new IllegalArgumentException("null not allowed");
        this.place = place;
    }

    public ThreeAddressCode get(int index) {
        return codes.get(index);
    }


    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        for (ThreeAddressCode code : codes) {
            String toString = code.toString();
            list.add(toString);
        }
        return String.join("\n", list);
    }

    public void add(ThreeAddressCodeList threeAddressCodeList) {
        this.codes.addAll(threeAddressCodeList.codes);
    }

    public void addCode(ThreeAddressCode threeAddressCode) {
            this.codes.add(threeAddressCode);
    }

    public boolean isEmpty() {
        return this.codes.isEmpty();
    }

    @Override
    public Iterator<ThreeAddressCode> iterator() {
        return codes.iterator();
    }

    @Override
    public void forEach(Consumer<? super ThreeAddressCode> action) {
        codes.forEach(action);
    }

    @Override
    public Spliterator<ThreeAddressCode> spliterator() {
        return codes.spliterator();
    }
}
