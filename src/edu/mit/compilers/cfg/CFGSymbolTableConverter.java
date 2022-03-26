package edu.mit.compilers.cfg;

import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.descriptors.MethodDescriptor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.symbolTable.SymbolTableType;

import java.util.HashMap;
import java.util.TreeSet;

public class CFGSymbolTableConverter {
    SymbolTable fields;
    SymbolTable methods;
    TreeSet<String> imports;

    HashMap<String, SymbolTable> cfgMethods = new HashMap<>();

    public CFGSymbolTableConverter(GlobalDescriptor globalDescriptor) {
        this.fields = globalDescriptor.globalVariablesSymbolTable;
        this.methods = globalDescriptor.methodsSymbolTable;
        this.imports = globalDescriptor.imports;
    }

    public HashMap<String, SymbolTable> createCFGSymbolTables() {
        for (HashMap.Entry<String, Descriptor> methodEntry : methods.entries.entrySet()) {
            // is this check necessary? does a method table only contain one entry of type MethodDescriptor?
            if (methodEntry.getValue() instanceof MethodDescriptor) {
                MethodDescriptor methodDesc = (MethodDescriptor) methodEntry.getValue();
                SymbolTable methodVars = new SymbolTable(null, SymbolTableType.Method);
                cfgMethods.put(methodEntry.getKey(), methodVars);

                // add all params to symbol table, don't need to check for repeats bc it's the first table
                methodVars.entries.putAll(methodDesc.parameterSymbolTable.entries);

                // iterate through all children of children blocks
                int uniqueIndex = 1;
                addChildrenVars(methodVars, methodDesc.localSymbolTable, uniqueIndex);
            }
        }
        return cfgMethods;
    }

    public void addChildrenVars(SymbolTable methodTable, SymbolTable currTable, int uniqueIndex) {
        for (HashMap.Entry<String, Descriptor> variable: currTable.entries.entrySet()) {
            // uniquely name each valid variable in method scope
            String varName = variable.getKey();
            if (methodTable.containsEntry(varName)) {
                varName += "" + uniqueIndex;
                uniqueIndex += 1;
            }
            methodTable.entries.put(varName, variable.getValue());
        }

        for (SymbolTable child : currTable.children)
            addChildrenVars(methodTable, child, uniqueIndex);
    }
}
