package decaf.cfg;


import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.TreeSet;

import decaf.ast.AST;
import decaf.ast.Block;
import decaf.ast.Name;
import decaf.common.Pair;
import decaf.descriptors.Descriptor;
import decaf.descriptors.GlobalDescriptor;
import decaf.descriptors.MethodDescriptor;
import decaf.symboltable.SymbolTable;
import decaf.symboltable.SymbolTableType;

public class SymbolTableFlattener {
  private final SymbolTable fields;
  private final SymbolTable methods;
  private final TreeSet<String> imports;

  private final HashMap<String, SymbolTable> cfgMethods = new HashMap<>();
  private int uniqueIndex;

  public SymbolTableFlattener(GlobalDescriptor globalDescriptor) {
    this.fields = globalDescriptor.globalVariablesSymbolTable;
    this.methods = globalDescriptor.methodsSymbolTable;
    this.imports = globalDescriptor.imports;
  }

  private static void renameVariableWithinCurrentScope(
      Block block,
      String oldLabel,
      String newLabel
  ) {
    Queue<AST> toExplore = new ArrayDeque<>(block.getChildren()
                                                 .stream()
                                                 .map(Pair::second)
                                                 .toList());
    while (!toExplore.isEmpty()) {
      AST ast = toExplore.remove();
      for (Pair<String, AST> astPair : ast.getChildren()) {
        AST child = astPair.second();
        if (child instanceof Name name) {
          if (name.getLabel()
                  .equals(oldLabel)) {
            name.setLabel(newLabel);
          }
        }
        if (!(child instanceof Block))
          toExplore.add(child);
      }
    }
  }

  private static void rename(
      Block block,
      String oldLabel,
      String newLabel
  ) {
    Queue<AST> toExplore = new ArrayDeque<>();
    toExplore.add(block);
    while (!toExplore.isEmpty()) {
      AST ast = toExplore.remove();
      for (Pair<String, AST> astPair : ast.getChildren()) {
        AST child = astPair.second();
        if (child instanceof Name name) {
          if (name.getLabel()
                  .equals(oldLabel)) {
            name.setLabel(newLabel);
          }
        }
        toExplore.add(child);
      }
    }
  }

  public HashMap<String, SymbolTable> createCFGSymbolTables() {
    for (HashMap.Entry<String, Descriptor> methodEntry : methods.entries.entrySet()) {
      // is this check necessary? does a method table only contain one entry of type MethodDescriptor?
      if (methodEntry.getValue() instanceof MethodDescriptor methodDesc) {
        SymbolTable methodVars = new SymbolTable(null,
                                                 SymbolTableType.Method,
                                                 methodDesc.methodDefinition.getBlock()
        );
        cfgMethods.put(
            methodEntry.getKey(),
            methodVars
        );

        // add all params to symbol table, don't need to check for repeats bc it's the first table
//                for (HashMap.Entry<String, Descriptor> paramEntry : new ArrayList<>(methodDesc.parameterSymbolTable.entries.entrySet())) {
//                    var paramName = paramEntry.getKey();
//                    var newParamName = paramName + "." + "local";
//                    methodDesc.parameterSymbolTable.entries.remove(paramName);
//                    methodDesc.parameterSymbolTable.entries.put(newParamName, paramEntry.getValue());
//                    methodVars.entries.put(newParamName, paramEntry.getValue());
//                    rename(methodDesc.methodDefinition.block, paramName, newParamName);
//                }
        methodVars.entries.putAll(methodDesc.parameterSymbolTable.entries);
        methodVars.entries.putAll(this.fields.entries);
        // iterate through all children of children blocks
        addChildrenVars(
            methodVars,
            methodDesc.methodDefinition.getBlock().blockSymbolTable
        );
        this.fields.entries.forEach(methodVars.entries::remove);
        methodVars.parent = this.fields;
      }
    }

    return cfgMethods;
  }

  public void addChildrenVars(
      SymbolTable methodTable,
      SymbolTable currTable
  ) {
    for (HashMap.Entry<String, Descriptor> variable : currTable.entries.entrySet()) {
      // uniquely name each valid irAssignableValue in method scope
      String varName = variable.getKey();
      String newVarName = varName;
      if (methodTable.containsEntry(varName)) {
        newVarName += uniqueIndex++;
      }
      methodTable.entries.put(
          newVarName,
          variable.getValue()
      );

      if (!varName.equals(newVarName)) {
        currTable.entries.remove(varName);
        currTable.entries.put(
            newVarName,
            variable.getValue()
        );
        renameVariableWithinCurrentScope(
            currTable.owner,
            varName,
            newVarName
        );
      }
    }

    for (SymbolTable child : currTable.children)
      addChildrenVars(
          methodTable,
          child
      );
  }
}
