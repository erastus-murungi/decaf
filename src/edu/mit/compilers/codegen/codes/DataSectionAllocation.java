package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.symbolTable.SymbolTable;

public class DataSectionAllocation extends ThreeAddressCode {
    public static final int DEFAULT_ALIGNMENT = 8;
    final VariableName variableName;
    int size;
    int alignment;
    BuiltinType type;

    public DataSectionAllocation(AST source, String comment, VariableName variableName, int size, BuiltinType builtinType) {
        super(source, comment);
        this.variableName = variableName;
        this.size = size;
        this.type  = builtinType;
        this.alignment = DEFAULT_ALIGNMENT;
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }

    @Override
    public String toString() {
        return String.format("%s.comm _%s,%s,%s %s %s", INDENT, variableName, size, alignment, DOUBLE_INDENT, getComment().get() + " " + type.getSourceCode());
    }
}
