package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ThreeAddressCodeList {
    public String place;
    private final List<ThreeAddressCode> codes;

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

    public ThreeAddressCodeList() {
        codes = new ArrayList<>();
        this.place = TemporaryNameGenerator.getNextTemporaryVariable();
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
        for (ThreeAddressCode threeAddressCode: threeAddressCodeList.codes) {
            if (!(threeAddressCode instanceof Original)) {
                this.codes.add(threeAddressCode);
            }
        }
    }

    public void addCode(ThreeAddressCode threeAddressCode) {
        if (!(threeAddressCode instanceof Original)) {
            this.codes.add(threeAddressCode);
        }
    }
}
