package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;

import java.util.Collections;
import java.util.List;

public class ArrayBoundsCheck extends ThreeAddressCode{
    public Label indexIsLessThanArraySize;
    public Label indexIsGTEZero;
    public ConstantName arraySize;
    public AbstractName arrayIndex;
    public ArrayAccess arrayAccess;

    public ArrayBoundsCheck(AST source, ArrayAccess arrayAccess,String comment, AbstractName arrayIndex, ConstantName arraySize, Label indexIsLessThanArraySize, Label indexIsGTEZero) {
        super(source, comment);
        this.arraySize = arraySize;
        this.indexIsLessThanArraySize = indexIsLessThanArraySize;
        this.indexIsGTEZero = indexIsGTEZero;
        this.arrayIndex = arrayIndex;
        this.arrayAccess = arrayAccess;
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
    public String toString() {
        return String.format("%sCheckBounds", DOUBLE_INDENT);
    }
}