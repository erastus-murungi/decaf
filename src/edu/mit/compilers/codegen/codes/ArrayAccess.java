package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.VariableName;

import java.util.Collections;
import java.util.List;

public class ArrayAccess extends ThreeAddressCode {
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
    public String toString() {
        return String.format("%sload %s[%s]", DOUBLE_INDENT, arrayName, accessIndex);
    }
}
