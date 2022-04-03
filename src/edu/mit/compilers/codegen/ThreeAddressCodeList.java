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

    public int size() {
        return codes.size();
    }

    public int flattenedSize() {
        return flatten().size();
    }

    public Optional<ThreeAddressCodeList> getNext() {
        return Optional.ofNullable(next);
    }

    public ThreeAddressCodeList flatten() {
        ThreeAddressCodeList flattenTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        ThreeAddressCodeList tacList = this;
        while (tacList.getNext().isPresent()) {
            flattenTACList.add(tacList);
            tacList = tacList.getNext()
                             .get();
        }
        flattenTACList.add(tacList);
        return flattenTACList;
    }


    public static ThreeAddressCodeList of(ThreeAddressCode code) {
        ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(UNDEFINED);
        threeAddressCodeList.addCode(code);
        return threeAddressCodeList;

    }

    public ThreeAddressCode last() {
        ThreeAddressCodeList flattened = flatten();
        return flattened.get(flattened.size() - 1);
    }

    public ThreeAddressCode first() {
        return codes.get(0);
    }

    public ThreeAddressCodeList setNext(ThreeAddressCodeList next) {
        ThreeAddressCodeList head = this;
        while (head.getNext().isPresent())
            head = head.getNext().get();
        head.next = next;
        return next;
    }


    public void prepend(ThreeAddressCode code) {
        codes.add(0, code);
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
        for (ThreeAddressCode code : flatten()) {
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
