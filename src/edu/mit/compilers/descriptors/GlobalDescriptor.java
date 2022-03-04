package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.symbolTable.SymbolTableType;

import java.util.*;


/**
 * <ul>
 *     <li> a symbol table for methods of parent class </li>
 *     <li> a symbol table for globally accessible fields</li>
 *     <li> an ordered set of imports </li>
 * </ul>
 */

public class GlobalDescriptor extends Descriptor {
    public SymbolTable globalVariablesSymbolTable;
    public SymbolTable methodsSymbolTable;
    public TreeSet<String> imports = new TreeSet<>();

    public GlobalDescriptor(BuiltinType builtinType, SymbolTable globalVariablesSymbolTable, SymbolTable methodsSymbolTable) {
        super(builtinType, "<program>");
        this.globalVariablesSymbolTable = globalVariablesSymbolTable;
        this.methodsSymbolTable = methodsSymbolTable;
    }

    public GlobalDescriptor() {
        this(BuiltinType.Undefined, new SymbolTable(null, SymbolTableType.Field), new SymbolTable(null, SymbolTableType.Method));
    }
}
