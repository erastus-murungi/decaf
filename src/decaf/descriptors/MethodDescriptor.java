package decaf.descriptors;

import decaf.symboltable.SymbolTable;
import decaf.ast.MethodDefinition;

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
    public SymbolTable parameterSymbolTable;
    public SymbolTable localSymbolTable;

    public MethodDescriptor(MethodDefinition methodDefinition, SymbolTable parameterSymbolTable, SymbolTable localSymbolTable) {
        super(methodDefinition.returnType, methodDefinition.methodName.getLabel());
        this.methodDefinition = methodDefinition;
        this.parameterSymbolTable = parameterSymbolTable;
        this.localSymbolTable = localSymbolTable;
    }
}
