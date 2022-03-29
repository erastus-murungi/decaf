package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ThreeAddressCodeList {
    public String place;
    private final List<ThreeAddressCode> codes;
    public static final String UNDEFINED = "Undefined";

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

    public ThreeAddressCodeList(String place, List<ThreeAddressCode> codes) {
        this.place = place;
        this.codes = codes;
    }

    public ThreeAddressCodeList(String place) {
        codes = new ArrayList<>();
        if (place == null)
            throw new IllegalArgumentException("null not allowed");
        this.place = place;
    }

    public ThreeAddressCode get(int index) {
        return codes.get(index);
    }

    public ThreeAddressCode getFromLast(int index) {
        return codes.get(codes.size() - 1 - index);
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
}
