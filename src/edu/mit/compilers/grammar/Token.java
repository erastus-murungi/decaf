package edu.mit.compilers.grammar;


public class Token {
    public TokenPosition tokenPosition;
    public TokenType tokenType;
    public String lexeme;

    public Token(TokenPosition tokenPosition, TokenType tokenType, String lexeme) {
        this.tokenPosition = tokenPosition;
        this.tokenType = tokenType;
        this.lexeme = lexeme;
    }

    public String lexeme() {
        return lexeme;
    }

    public TokenType tokenType() {
        return tokenType;
    }

    public TokenPosition tokenPosition() {
        return tokenPosition;
    }

    public boolean isNotEOF() {
        return tokenType != TokenType.EOF;
    }

    @Override
    public String toString() {
        return "Token{" + "type=" + tokenType +
                ", lexeme='" + lexeme + '\'' +
                '}';
    }
}
