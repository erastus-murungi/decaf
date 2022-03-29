package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.Name;
import edu.mit.compilers.descriptors.*;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.symbolTable.SymbolTableType;
import edu.mit.compilers.utils.Pair;

import java.util.HashMap;
import java.util.Stack;
import java.util.TreeSet;

public class CFGSymbolTableConverter {
    SymbolTable fields;
    SymbolTable methods;
    TreeSet<String> imports;

    HashMap<String, SymbolTable> cfgMethods = new HashMap<>();
    int uniqueIndex;

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
                SymbolTable methodVars = new SymbolTable(null, SymbolTableType.Method, methodDesc.methodDefinition.block);
                cfgMethods.put(methodEntry.getKey(), methodVars);

                // add all params to symbol table, don't need to check for repeats bc it's the first table
                methodVars.entries.putAll(methodDesc.parameterSymbolTable.entries);
                methodVars.entries.putAll(this.fields.entries);
                // iterate through all children of children blocks
                addChildrenVars(methodVars, methodDesc.methodDefinition.block.blockSymbolTable);
                this.fields.entries.forEach(methodVars.entries::remove);
                methodVars.parent = this.fields;
            }
        }

        return cfgMethods;
    }

    public void addChildrenVars(SymbolTable methodTable, SymbolTable currTable) {
        for (HashMap.Entry<String, Descriptor> variable: currTable.entries.entrySet()) {
            // uniquely name each valid variable in method scope
            String varName = variable.getKey();
            String newVarName = varName;
            if (methodTable.containsEntry(varName)) {
                newVarName += uniqueIndex++;
            }
            methodTable.entries.put(newVarName, variable.getValue());

            if (!varName.equals(newVarName)) {
                currTable.entries.remove(varName);
                currTable.entries.put(newVarName, variable.getValue());
                renameVariableWithinCurrentScope(currTable.owner, varName, newVarName);
            }
        }

        for (SymbolTable child : currTable.children)
            addChildrenVars(methodTable, child);
    }

    private static void renameVariableWithinCurrentScope(Block block, String oldName, String newName) {
        Stack<AST> toExplore = new Stack<>();
        toExplore.push(block);
        while (!toExplore.isEmpty()) {
            AST ast = toExplore.pop();
            for (Pair<String, AST> astPair : ast.getChildren()) {
                AST child = astPair.second();
                if (child instanceof Name) {
                    Name name = (Name) child;
                    if (name.id.equals(oldName)) {
                        name.id = newName;
                    }
                }
                if (!(child instanceof Block))
                    toExplore.push(child);
            }
        }

    }
}
