package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.symbolTable.SymbolTable;

public class PopParameter extends ThreeAddressCode {
    AssignableName parameterName;

    public PopParameter(AssignableName parameterName, AST source) {
        super(source);
        this.parameterName = parameterName;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "PopParameter", parameterName);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}
