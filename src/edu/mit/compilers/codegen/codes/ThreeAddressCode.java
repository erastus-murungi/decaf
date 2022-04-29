package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ThreeAddressCode {
    public AST source;
    private String comment;
    public static final String INDENT = "    ";
    public static final String DOUBLE_INDENT = INDENT + INDENT;

    public ThreeAddressCode(AST source) {
        this.source = source;
    }

    public ThreeAddressCode(AST source, String comment) {
        this.comment = comment;
        this.source = source;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public abstract <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra);

    public abstract List<AbstractName> getNames();

    public List<AbstractName> getAssignableNames() {
        return getNames().stream()
                .filter(abstractName -> (abstractName instanceof AssignableName))
                .collect(Collectors.toList());
    }

    public abstract String repr();
}
