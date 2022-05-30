package edu.mit.compilers.ast;

import java.util.Objects;

import edu.mit.compilers.grammar.TokenPosition;

public abstract class IntLiteral extends Literal {

    public IntLiteral(TokenPosition tokenPosition, String literalToken) {
        super(tokenPosition, literalToken);
    }

    abstract public Long convertToLong();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntLiteral intLiteral = (IntLiteral) o;
        return Objects.equals(convertToLong(), intLiteral.convertToLong());
    }

    @Override
    public int hashCode() {
        return Objects.hash(convertToLong());
    }

    @Override
    public Type getType() {
        return Type.Int;
    }
}