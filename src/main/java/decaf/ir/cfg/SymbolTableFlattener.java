package decaf.ir.cfg;


import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.TreeSet;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.RValue;
import decaf.shared.Pair;
import decaf.shared.descriptors.Descriptor;
import decaf.shared.env.TypingContext;
import decaf.shared.descriptors.MethodDescriptor;
import decaf.shared.env.Scope;

public class SymbolTableFlattener {
  private final Scope fields;
  private final TreeSet<String> imports;

  private final HashMap<String, Scope> cfgMethods = new HashMap<>();
  private int uniqueIndex;

  public SymbolTableFlattener(TypingContext typingContext) {
    this.fields = typingContext.globalScope;
    this.imports = typingContext.imports;
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
        if (child instanceof RValue RValue) {
          if (RValue.getLabel()
                    .equals(oldLabel)) {
            RValue.setLabel(newLabel);
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
        if (child instanceof RValue RValue) {
          if (RValue.getLabel()
                    .equals(oldLabel)) {
            RValue.setLabel(newLabel);
          }
        }
        toExplore.add(child);
      }
    }
  }

  public HashMap<String, Scope> createCFGSymbolTables() {
    for (HashMap.Entry<String, Descriptor> methodEntry : fields.entrySet()) {
      // is this check necessary? does a method table only contain one entry of type MethodDescriptor?
      if (methodEntry.getValue() instanceof MethodDescriptor methodDesc) {
        Scope methodVars = new Scope(
            null,
            Scope.For.Field,
            methodDesc.methodDefinition.getBody()
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
        methodVars.putAll(methodDesc.formalAgumentsScope);
        methodVars.putAll(this.fields);
        // iterate through all children of children blocks
        addChildrenVars(
            methodVars,
            methodDesc.methodDefinition.getBody().scope
        );
        this.fields.forEach(methodVars::remove);
        methodVars.parent = this.fields;
      }
    }

    return cfgMethods;
  }

  public void addChildrenVars(
      Scope methodTable,
      Scope currTable
  ) {
    for (HashMap.Entry<String, Descriptor> variable : currTable.entrySet()) {
      // uniquely name each valid irAssignableValue in method scope
      String varName = variable.getKey();
      String newVarName = varName;
      if (methodTable.containsKey(varName)) {
        newVarName += uniqueIndex++;
      }
      methodTable.put(
          newVarName,
          variable.getValue()
      );

      if (!varName.equals(newVarName)) {
        currTable.remove(varName);
        currTable.put(
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

    for (Scope child : currTable.children)
      addChildrenVars(
          methodTable,
          child
      );
  }
}
