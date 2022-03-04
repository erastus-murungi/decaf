package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.symbolTable.SymbolTable; 

/**
 * A method descriptor contains:
 *
 * <ul>
 *      <li> a reference to code for method (methodDefinition)
 *
 *      <li> a reference to local symbol table (localSymbolTable)
 *
 *      <li> a reference to the method parameters symbol table (parameterSymbolTable)
 *</ul>
 */
public class MethodDescriptor extends Descriptor {
    public MethodDefinition methodDefinition;
    public SymbolTable parameterSymbolTable;
    public SymbolTable localSymbolTable;

    public MethodDescriptor(MethodDefinition methodDefinition, SymbolTable parameterSymbolTable, SymbolTable localSymbolTable) {
        super(methodDefinition.returnType, methodDefinition.methodName.id);
        this.methodDefinition = methodDefinition;
        this.parameterSymbolTable = parameterSymbolTable;
        this.localSymbolTable = localSymbolTable;
    }
}
