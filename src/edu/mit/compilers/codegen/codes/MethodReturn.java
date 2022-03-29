package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MethodReturn extends ThreeAddressCode {
    private AssignableName returnAddress;
    public MethodReturn(AST source) {
        super(source);
    }

    public MethodReturn(AST source, AssignableName returnAddress) {
        super(source);
        this.returnAddress = returnAddress;
    }

    public Optional<AssignableName> getReturnAddress() {
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

    @Override
    public List<AbstractName> getNames() {
        return List.of(returnAddress);
    }
}
