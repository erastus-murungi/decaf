package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Value;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class Instruction {
    public AST source;
    private String comment;
    public static final String INDENT = "    ";
    public static final String DOUBLE_INDENT = INDENT + INDENT;

    public Instruction(AST source, String comment) {
        this.comment = comment;
        this.source = source;
    }

    public Instruction(AST source) {
        this(source, null);
    }

    public Instruction() {
        this(null, null);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public abstract <T, E> T accept(InstructionVisitor<T, E> visitor, E extra);

    public abstract List<Value> getAllNames();

    public abstract String syntaxHighlightedToString();

    public abstract Instruction copy();

    public Collection<LValue> getAllLValues() {
        return getAllNames().stream()
                            .filter(value -> (value instanceof LValue))
                            .map(value -> (LValue) value)
                            .collect(Collectors.toList());
    }
}
