package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.ArrayAccess;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThreeAddressCodeList implements Iterable<ThreeAddressCode>, Cloneable {
    public static final AbstractName UNDEFINED = new VariableName(null, -1, null);

    public AbstractName place;

    private List<ThreeAddressCode> codes;

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
        while (tacList
                .getNext()
                .isPresent()) {
            flattenTACList.add(tacList);
            tacList = tacList
                    .getNext()
                    .get();
        }
        flattenTACList.add(tacList);
        return flattenTACList;
    }

    public void reset(Collection<ThreeAddressCode> newCodes) {
        codes.clear();
        codes.addAll(newCodes);
    }

    public void replaceIfContainsOldCodeAtIndex(int indexOfOldCode, ThreeAddressCode oldCode, ThreeAddressCode newCode) {
        if (codes.get(indexOfOldCode) != oldCode) {
            throw new IllegalArgumentException(oldCode + "not found in TAC");
        }
        codes.set(indexOfOldCode, newCode);
    }


    public static ThreeAddressCodeList of(ThreeAddressCode code) {
        ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(UNDEFINED);
        threeAddressCodeList.addCode(code);
        return threeAddressCodeList;
    }

    public static ThreeAddressCodeList of(ThreeAddressCode code, AssignableName place) {
        ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(place);
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
        while (head
                .getNext()
                .isPresent())
            head = head
                    .getNext()
                    .get();
        head.next = next;
        return next;
    }

    public void addLast(ThreeAddressCode tac) {
        ThreeAddressCodeList head = this;
        while (head
                .getNext()
                .isPresent())
            head = head
                    .getNext()
                    .get();
        head.addCode(tac);
    }


    public void prepend(ThreeAddressCode code) {
        codes.add(0, code);
    }

    public ThreeAddressCodeList(AbstractName place, List<ThreeAddressCode> codes) {
        this.place = place;
        this.codes = codes;
    }

    public static ThreeAddressCodeList empty() {
        return new ThreeAddressCodeList(UNDEFINED);
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
        return flatten().getCodes().stream().filter(code -> !(code instanceof ArrayAccess)).map(ThreeAddressCode::repr).collect(Collectors.joining("\n"));
    }

    public void add(ThreeAddressCodeList threeAddressCodeList) {
        this.codes.addAll(threeAddressCodeList.codes);
    }

    public void add(Collection<ThreeAddressCode> codes) {
        this.codes.addAll(codes);
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

    @Override
    public ThreeAddressCodeList clone() {
        try {
            ThreeAddressCodeList clone = (ThreeAddressCodeList) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.codes = new ArrayList<>(codes);
            clone.place = place;
            clone.next = next;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public List<ThreeAddressCode> getCodes() {
        return codes;
    }

    public void prependAll(ThreeAddressCodeList threeAddressCodeList) {
        codes.addAll(0, threeAddressCodeList.codes);
    }
}
