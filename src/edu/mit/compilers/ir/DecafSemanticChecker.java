package edu.mit.compilers.ir;

import edu.mit.compilers.ast.AST;

public class DecafSemanticChecker {

    private boolean hasError;

    public DecafSemanticChecker(AST rootNode) {
        IRVisitor irVistor = new IRVisitor();
        rootNode.accept(irVistor, null);
        hasError = irVistor.exceptions.size() > 0;
    }

    public boolean hasError() {
        return hasError;
    }
}
