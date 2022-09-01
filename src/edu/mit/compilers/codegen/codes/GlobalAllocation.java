package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;

import java.util.Collections;
import java.util.List;

public class GlobalAllocation extends Instruction {
    public static final int DEFAULT_ALIGNMENT = 8;

    public final LValue variableName;
    public final long size;
    public final int alignment;
    public final Type type;

    public GlobalAllocation(AST source, String comment, LValue variableName, long size, Type type) {
        super(source, comment);
        this.variableName = variableName;
        this.size = size;
        this.type  = type;
        this.alignment = DEFAULT_ALIGNMENT;
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllValues() {
        return Collections.singletonList(variableName);
    }

    @Override
    public String syntaxHighlightedToString() {
        return String.format("global %s %s", variableName.getType().getSourceCode(), variableName.repr());
    }

    @Override
    public Instruction copy() {
        return new GlobalAllocation(source, getComment().orElse(null), variableName, size, type);
    }

    @Override
    public String toString() {
        return String.format("%s.comm %s,%s,%s %s %s", INDENT, variableName, size, alignment, DOUBLE_INDENT, getComment().orElse(" ") + " " + type.getSourceCode());
    }
}
