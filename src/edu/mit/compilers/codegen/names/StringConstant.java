package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;

public class StringConstant extends Constant {
    private final String content;
    private final String contentEscaped;

    public StringConstant(String content) {
        super(Type.String, TemporaryNameIndexGenerator.getNextStringLiteralIndex());
        this.content = content;
        this.contentEscaped = content.substring(1, content.length() - 1).translateEscapes();
    }

    private StringConstant(String label, String content, String contentEscaped) {
        super(Type.String, label);
        this.content = content;
        this.contentEscaped = contentEscaped;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StringConstant that = (StringConstant) o;
        return Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public StringConstant copy() {
        return new StringConstant(label, content, contentEscaped);
    }

    public int size() {
        return contentEscaped.length();
    }

    @Override
    public String repr() {
        return String.format("@.%s", getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel());
    }

    @Override
    public void renameForSsa(int versionNumber) {
        throw new IllegalStateException();
    }

    @Override
    public String getValue() {
        return content;
    }

}
