package edu.mit.compilers.symbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import edu.mit.compilers.descriptors.Descriptor;


public class SymbolTable {
    public final SymbolTable parent;
    public final SymbolTableType symbolTableType;
    public final HashMap<String, Descriptor> entries = new HashMap<>();
    public ArrayList<SymbolTable> children = new ArrayList<>();

    public SymbolTable(SymbolTable parent, SymbolTableType symbolTableType) {
        super();
        this.parent = parent;
        this.symbolTableType = symbolTableType;
    }

    /**
     * Look up a variable only within the current scope
     *
     * @param stringId the id to lookup in the symbol table hierarchy
     * @return Optional.empty if the descriptor is not found else Optional[Descriptor]
     */

    public Optional<Descriptor> getDescriptorFromCurrentScope(String stringId) {
        Descriptor currentDescriptor = entries.get(stringId);
        if (currentDescriptor == null) {
            if (parent != null)
                return parent.getDescriptorFromValidScopes(stringId);
        }
        return Optional.ofNullable(currentDescriptor);
    }

    /**
     * @param key the id to lookup in the symbol table hierarchy
     * @return true if the descriptor for key is found else false
     */

    public boolean containsEntry(String key) {
        return entries.containsKey(key);
    }

    /**
     * Look up a variable recursively up the scope hierarchy
     *
     * @param stringId the id to lookup in the symbol table hierarchy
     * @return Optional.empty if the descriptor is not found else Optional[Descriptor]
     */
    public Optional<Descriptor> getDescriptorFromValidScopes(String stringId) {
        Descriptor currentDescriptor = entries.get(stringId);
        if (currentDescriptor == null) {
            if (parent != null)
                return parent.getDescriptorFromValidScopes(stringId);
        }
        return Optional.ofNullable(currentDescriptor);
    }

    /**
     * Look up a variable recursively up the scope hierarchy to see is there is incorrect shadowing parameter
     *
     * @param stringId the id to lookup in the symbol table hierarchy
     * @return true if there is incorrect shadowing of parameter and false otherwise
     */
    public boolean isShadowingParameter(String stringId) {
        Descriptor currentDescriptor = entries.get(stringId);
        if (parent == null) {
            return currentDescriptor != null && symbolTableType == SymbolTableType.Parameter;
        } else {
            if (currentDescriptor != null && symbolTableType == SymbolTableType.Parameter)
                return true;
            else
                return parent.isShadowingParameter(stringId);
        }
    }

    public String toString() {
        String output = "";
        for (Map.Entry<String, Descriptor> entry: entries.entrySet()) {
            output += entry.getKey();
            output += "\n";
        }
        return output;
    }
}
