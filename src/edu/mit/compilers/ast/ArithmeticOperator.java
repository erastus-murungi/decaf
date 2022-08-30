package edu.mit.compilers.ast;


import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class ArithmeticOperator extends BinOperator {
    public ArithmeticOperator(TokenPosition tokenPosition, @DecafScanner.ArithmeticOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (label) {
            case DecafScanner.PLUS: return "Add";
            case DecafScanner.MINUS: return "Sub";
            case DecafScanner.MULTIPLY: return "Multiply";
            case DecafScanner.DIVIDE: return "Divide";
            case DecafScanner.MOD: return "Mod";
            default: throw new IllegalArgumentException("please register a display string for: " + label);
        }
    }

    @Override
    public Type getType() {
        return Type.Undefined;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (label) {
            case DecafScanner.PLUS: return "+";
            case DecafScanner.MINUS: return "-";
            case DecafScanner.MULTIPLY: return "*";
            case DecafScanner.DIVIDE: return "/";
            case DecafScanner.MOD: return "%";
            default: throw new IllegalArgumentException("please register a display string for: " + label);
        }
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return null;
    }
}
