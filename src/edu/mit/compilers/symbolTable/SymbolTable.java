package edu.mit.compilers.symbolTable;

import java.util.HashMap;

public class SymbolTable<String, Descriptor> extends HashMap<String, Descriptor> {
    private final SymbolTable<String, Descriptor> parent;

    public SymbolTable(SymbolTable<String, Descriptor> parent) {
        super();
        this.parent = parent;
    }

    // value can be the actual value of a variable or another SymbolTable
    public void addEntry(String key, Descriptor value) {
        put(key, value);
    }

    public boolean containsEntry(String key) {
        return containsKey(key);
    }

    public void updateEntry(String key, Descriptor newValue) {
        put(key, newValue);
    }

    public Object getEntryValue(String key) {
        return get(key);
    }

    public SymbolTable<String, Descriptor> getParent() {
        return this.parent;
    }
}
