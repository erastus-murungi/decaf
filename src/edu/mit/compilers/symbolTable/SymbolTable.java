package edu.mit.compilers.symbolTable;

import java.util.HashMap;

import edu.mit.compilers.descriptors.Descriptor;


public class SymbolTable {
    private final SymbolTable parent;
    private final SymbolTableType symbolTableType;
    private final HashMap<String, Descriptor> entries = new HashMap<>();

    public SymbolTable(SymbolTable parent, SymbolTableType symbolTableType) {
        super();
        this.parent = parent;
        this.symbolTableType = symbolTableType;
    }

    // value can be the actual value of a variable or another SymbolTable
    public void addEntry(String key, Descriptor value) {
        entries.put(key, value);
    }

    public boolean containsEntry(String key) {
        return entries.containsKey(key);
    }

    public void updateEntry(String key, Descriptor newValue) {
        entries.put(key, newValue);
    }

    public Object getEntryValue(String key) {
        return entries.get(key);
    }

    public SymbolTable getParent() {
        return this.parent;
    }
}
