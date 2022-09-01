package edu.mit.compilers.descriptors;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.symboltable.SymbolTableType;

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

    public GlobalDescriptor(Type type, SymbolTable globalVariablesSymbolTable, SymbolTable methodsSymbolTable, TreeSet<String> importDeclarations) {
        super(type, "<program>");
        this.globalVariablesSymbolTable = globalVariablesSymbolTable;
        this.methodsSymbolTable = methodsSymbolTable;
        this.imports.addAll(importDeclarations);
    }

    public GlobalDescriptor() {
        this(Type.Undefined, new SymbolTable(null, SymbolTableType.Field, null), new SymbolTable(null, SymbolTableType.Method, null), new TreeSet<>());
    }
}
