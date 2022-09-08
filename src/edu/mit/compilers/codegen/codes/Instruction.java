package edu.mit.compilers.codegen.codes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.Variable;

public abstract class Instruction {
    public static final String INDENT = "    ";
    public static final String DOUBLE_INDENT = INDENT + INDENT;
    public AST source;
    private String comment;

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

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public abstract <T, E> void accept(AsmWriter asmWriter);

    public abstract List<Value> getAllValues();

    public abstract String syntaxHighlightedToString();

    public abstract Instruction copy();

    public Collection<LValue> getAllLValues() {
        return getAllValues().stream()
                .filter(value -> (value instanceof LValue))
                .map(value -> (LValue) value)
                .collect(Collectors.toList());
    }

    public Collection<Variable> getAllScalarVariables() {
        return getAllValues().stream()
                .filter(value -> (value instanceof Variable))
                .map(value -> (Variable) value)
                .collect(Collectors.toList());

    }

    public String noCommentsSyntaxHighlighted() {
        if (getComment().isPresent()) {
            String comment = getComment().get();
            setComment(null);
            var res = syntaxHighlightedToString();
            setComment(comment);
            return res;
        }
        return syntaxHighlightedToString();
    }

    public String noCommentsToString() {
        if (getComment().isPresent()) {
            String comment = getComment().get();
            setComment(null);
            var res = toString();
            setComment(comment);
            return res;
        }
        return toString();
    }
}
