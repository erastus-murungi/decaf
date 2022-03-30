package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MethodBegin extends ThreeAddressCode {
    public ConstantName sizeOfLocals;
    public final MethodDefinition methodDefinition;
    private List<AbstractName> locals;
    public HashMap<String, Integer> nameToStackOffset = new HashMap<>();

    public MethodBegin(MethodDefinition methodDefinition) {
        super(methodDefinition);
        this.methodDefinition = methodDefinition;
        this.sizeOfLocals = null;
    }

    @Override
    public String toString() {
        return String.format("%s:\n%s%s %s", methodDefinition.methodName.id, DOUBLE_INDENT, "BeginFunction", sizeOfLocals);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.emptyList();
    }

    public void setLocals(List<AbstractName> locals) {
        this.locals = locals;
        this.sizeOfLocals = new ConstantName(
                2* (long) locals.stream().map(abstractName -> abstractName.size).reduce(0, Integer::sum), BuiltinType.Int.getFieldSize());
    }

    public List<AbstractName> getLocals() {
        return locals;
    }
}
