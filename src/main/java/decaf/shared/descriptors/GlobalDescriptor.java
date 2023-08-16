package decaf.shared.descriptors;


import java.util.TreeSet;

import decaf.shared.env.Scope;


/**
 * <ul>
 *     <li> a symbol table for methods of parent class </li>
 *     <li> a symbol table for globally accessible fields</li>
 *     <li> an ordered set of imports </li>
 * </ul>
 */

public class GlobalDescriptor extends Descriptor {
  public Scope globalVariablesScope;
  public Scope methodsScope;
  public TreeSet<String> imports = new TreeSet<>();

  public GlobalDescriptor(
      decaf.analysis.syntax.ast.Type type,
      Scope globalVariablesScope,
      Scope methodsScope,
      TreeSet<String> importDeclarations
  ) {
    super(
        type,
        "<program>"
    );
    this.globalVariablesScope = globalVariablesScope;
    this.methodsScope = methodsScope;
    this.imports.addAll(importDeclarations);
  }

  public GlobalDescriptor() {
    this(
        decaf.analysis.syntax.ast.Type.Undefined,
        new Scope(
            null,
            Scope.For.Field,
            null
        ),
        new Scope(
            null,
            Scope.For.Method,
            null
        ),
        new TreeSet<>()
    );
  }
}
