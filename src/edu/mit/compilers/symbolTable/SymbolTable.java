package edu.mit.compilers.symbolTable;

import java.util.HashMap;

import edu.mit.compilers.descriptors.Descriptor;


public class SymbolTable {
    public final SymbolTable parent;
    public final SymbolTableType symbolTableType;
    public final HashMap<String, Descriptor> entries = new HashMap<>();

    public SymbolTable(SymbolTable parent, SymbolTableType symbolTableType) {
        super();
        this.parent = parent;
        this.symbolTableType = symbolTableType;
    }

    // TODO: looking recursively at parents
    public boolean containsEntry(String key) {
        return entries.containsKey(key);
    }
}
