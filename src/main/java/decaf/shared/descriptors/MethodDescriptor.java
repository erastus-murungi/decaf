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
    public Scope formalAgumentsScope;
    public Scope localScope;

    public MethodDescriptor(MethodDefinition methodDefinition, Scope formalAgumentsScope, Scope localScope) {
        super(methodDefinition.getReturnType(), From.METHOD);
        this.methodDefinition = methodDefinition;
        this.formalAgumentsScope = formalAgumentsScope;
        this.localScope = localScope;
    }
}
