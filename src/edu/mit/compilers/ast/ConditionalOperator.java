package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symboltable.SymbolTable;

public class ConditionalOperator extends BinOperator {
    public ConditionalOperator(TokenPosition tokenPosition, @DecafScanner.ConditionalOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (label) {
            case DecafScanner.CONDITIONAL_OR:
                return "Or";
            case DecafScanner.CONDITIONAL_AND:
                return "And";
            default:
                throw new IllegalArgumentException("please register conditional operator: " + label);
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (label) {
            case DecafScanner.CONDITIONAL_OR:
                return "||";
            case DecafScanner.CONDITIONAL_AND:
                return "&&";
            default:
                throw new IllegalArgumentException("please register conditional operator: " + label);
        }
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return null;
    }
}