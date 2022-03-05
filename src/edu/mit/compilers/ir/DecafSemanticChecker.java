package edu.mit.compilers.ir;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Program;

public class DecafSemanticChecker {

    private boolean hasError;

    public DecafSemanticChecker(AST rootNode) {
        IRVisitor irVistor = new IRVisitor();
        rootNode.accept(irVistor, null);
        TypeCheckVisitor typeCheckVisitor = new TypeCheckVisitor((Program) rootNode, irVistor.methods, irVistor.fields, irVistor.imports);
        rootNode.accept(typeCheckVisitor, irVistor.fields);
        hasError = irVistor.exceptions.size() > 0;
    }

    public boolean hasError() {
        return hasError;
    }
}
