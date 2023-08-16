package decaf.shared.descriptors;


import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.shared.env.Scope;

/**
 * A method descriptor contains:
 *
 * <ul>
 *      <li> a reference to code for method (methodDefinition)
 *
 *      <li> a reference to local symbol table (localSymbolTable)
 *
 *      <li> a reference to the method parameters symbol table (parameterSymbolTable)
 * </ul>
 */
public class MethodDescriptor extends Descriptor {
  public MethodDefinition methodDefinition;
  public Scope parameterScope;
  public Scope localScope;

  public MethodDescriptor(
      MethodDefinition methodDefinition,
      Scope parameterScope,
      Scope localScope
  ) {
    super(
        methodDefinition.getReturnType(),
        methodDefinition.getMethodName()
                        .getLabel()
    );
    this.methodDefinition = methodDefinition;
    this.parameterScope = parameterScope;
    this.localScope = localScope;
  }
}
