package decaf.ast;


import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class ArithmeticOperator extends BinOperator {
    public ArithmeticOperator(TokenPosition tokenPosition, @DecafScanner.ArithmeticOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (label) {
            case DecafScanner.PLUS:
                return "Add";
            case DecafScanner.MINUS:
                return "Sub";
            case DecafScanner.MULTIPLY:
                return "Multiply";
            case DecafScanner.DIVIDE:
                return "Divide";
            case DecafScanner.MOD:
                return "Mod";
            default:
                throw new IllegalArgumentException("please register a display string for: " + label);
        }
    }

    @Override
    public Type getType() {
        return Type.Undefined;
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (label) {
            case DecafScanner.PLUS:
                return "+";
            case DecafScanner.MINUS:
                return "-";
            case DecafScanner.MULTIPLY:
                return "*";
            case DecafScanner.DIVIDE:
                return "/";
            case DecafScanner.MOD:
                return "%";
            default:
                throw new IllegalArgumentException("please register a display string for: " + label);
        }
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return null;
    }
}
