package edu.mit.compilers.symbolTable;

import java.util.HashMap;

public class SymbolTable {
    private SymbolTable parent;
    private HashMap<String, Object> entries;

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
        entries = new HashMap<>();
    }

    // value can be the actual value of a variable or another SymbolTable
    public void addEntry(String key, Object value) {
        entries.put(key, value);
    }

    public boolean containsEntry(String key) {
        return entries.containsKey(key);
    }

    public void updateEntry(String key, Object newValue) {
        entries.put(key, newValue);
    }

    public Object getEntryValue(String key) {
        return entries.get(key);
    }

    public SymbolTable getParent() {
        return this.parent;
    }
}
