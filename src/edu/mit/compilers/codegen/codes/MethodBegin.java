package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MethodBegin extends ThreeAddressCode {
    public long sizeOfLocals;
    public final MethodDefinition methodDefinition;
    private List<AbstractName> locals;

    public MethodBegin(MethodDefinition methodDefinition) {
        super(methodDefinition);
        this.methodDefinition = methodDefinition;
        this.sizeOfLocals = -1;
    }

    @Override
    public String toString() {
        return String.format("%s:\n%s%s %s", methodDefinition.methodName.id, DOUBLE_INDENT, "BeginFunction", sizeOfLocals);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.emptyList();
    }

    public void setLocals(List<AbstractName> locals) {
        this.locals = locals;
        this.sizeOfLocals = locals.stream().map(abstractName -> abstractName.size).reduce(0, Integer::sum);
    }

    public Optional<List<AbstractName>> getLocals() {
        return Optional.ofNullable(locals);
    }
}
