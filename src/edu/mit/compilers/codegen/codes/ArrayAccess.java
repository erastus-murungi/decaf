package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayAccess extends ThreeAddressCode implements HasOperand {
    public ArrayName arrayName;
    public ConstantName arrayLength;
    public AbstractName accessIndex;

    public ArrayAccess(AST source,
                       String comment,
                       ArrayName arrayName,
                       ConstantName arrayLength,
                       AbstractName accessIndex) {
        super(source, comment);
        this.arrayName = arrayName;
        this.arrayLength = arrayLength;
        this.accessIndex = accessIndex;
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.emptyList();
    }

    @Override
    public String repr() {
        return String.format("%sload %s[%s]", DOUBLE_INDENT, arrayName, accessIndex.repr());
    }

    @Override
    public String toString() {
        return String.format("%sload %s[%s]", DOUBLE_INDENT, arrayName, accessIndex.repr());
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(accessIndex);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(accessIndex);
    }

    @Override
    public List<AbstractName> getOperandNamesNoArray() {
        return getOperandNames().stream().filter(abstractName -> !(abstractName instanceof ArrayName)).collect(Collectors.toList());
    }

    @Override
    public boolean hasUnModifiedOperand() {
        return true;
    }

    public boolean replace(AbstractName oldVariable, AbstractName replacer) {
        var replaced = false;
        if (accessIndex.equals(oldVariable)) {
            accessIndex = replacer;
            replaced = true;
        }
        return replaced;
    }

}
