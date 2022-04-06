package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Expression;

public class CFGExpression extends CFGLine {
    public CFGExpression(AST ast) {
        super(ast instanceof Expression ? iCFGVisitor.rotateBinaryOpExpression((Expression) ast) : ast);
        System.out.println("in class " + this.ast);
    }
}
