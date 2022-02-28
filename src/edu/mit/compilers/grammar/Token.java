package edu.mit.compilers.grammar;

public record Token(TokenPosition tokenPosition, TokenType tokenType, String lexeme) {
    public boolean isNotEOF() {
        return tokenType != TokenType.EOF;
    }
}