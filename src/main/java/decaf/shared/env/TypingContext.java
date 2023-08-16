package decaf.shared.env;


import java.util.TreeSet;


/**
 * <ul>
 *     <li> a symbol table for methods of parent class </li>
 *     <li> a symbol table for globally accessible fields</li>
 *     <li> an ordered set of imports </li>
 * </ul>
 */

public class TypingContext {
  public Scope globalScope;
  public TreeSet<String> imports = new TreeSet<>();

  public TypingContext(
      Scope globalScope,
      TreeSet<String> importDeclarations
  ) {
    this.globalScope = globalScope;
    this.imports.addAll(importDeclarations);
  }

}
