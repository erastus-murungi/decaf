package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.List;

public class ThreeAddressCodeList {
    public String place;
    private List<ThreeAddressCode> codes;

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
        return "ThreeAddressCodeList: \n" +
                "place: " + place + "\n" +
                "codes: \n" + String.join("\n", list);
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
