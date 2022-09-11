package edu.mit.compilers.codegen.codes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Value;

public class GlobalAllocation extends Instruction {
    public static final int DEFAULT_ALIGNMENT = 8;

    private final LValue value;
    private final long size;
    public final int alignment;
    public final Type type;

    public LValue getValue() {
        return value;
    }

    public long getSize() {
        return size;
    }

    public GlobalAllocation(@NotNull LValue value, long size, @NotNull Type type, @Nullable AST source, @Nullable String comment) {
        super(source, comment);
        this.value = value;
        this.size = size;
        this.type = type;
        this.alignment = DEFAULT_ALIGNMENT;
    }

    @Override
    public void accept(@NotNull AsmWriter asmWriter) {
    }

    @Override
    public List<Value> getAllValues() {
        return Collections.singletonList(value);
    }

    @Override
    public String syntaxHighlightedToString() {
        return String.format("global %s %s", value.getType().getSourceCode(), value.repr());
    }

    @Override
    public Instruction copy() {
        return new GlobalAllocation(value, size, type, source, getComment().orElse(null));
    }

    @Override
    public String toString() {
        return String.format("%s.comm %s,%s,%s %s %s", INDENT, value, size, alignment, DOUBLE_INDENT, getComment().orElse(" ") + " " + type.getSourceCode());
    }
}
