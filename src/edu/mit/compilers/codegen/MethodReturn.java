package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.Optional;

public class MethodReturn extends ThreeAddressCode {
    private String returnAddress;
    public MethodReturn(AST source) {
        super(source);
    }

    public MethodReturn(AST source, String returnAddress) {
        super(source);
        this.returnAddress = returnAddress;
    }

    public Optional<String> getReturnAddress() {
        return Optional.ofNullable(returnAddress);
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "Return", getReturnAddress().isEmpty() ? " " : getReturnAddress().get());
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}
