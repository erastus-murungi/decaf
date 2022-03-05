package edu.mit.compilers.ir;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;


public class DecafSemanticChecker {

    private boolean trace;

    private boolean hasError;

    private AST rootNode;

    public DecafSemanticChecker(Program rootNode) {
        this.rootNode = rootNode;
    }

    public DecafSemanticChecker(AST rootNode) {
        this.rootNode = rootNode;
    }

    public void runChecks(DecafScanner decafScanner) {
        IRVisitor irVisitor = new IRVisitor();
        rootNode.accept(irVisitor, null);
        TypeCheckVisitor typeCheckVisitor = new TypeCheckVisitor((Program) rootNode, irVisitor.methods, irVisitor.fields, irVisitor.imports);
        rootNode.accept(typeCheckVisitor, irVisitor.fields);
        hasError = Visitor.exceptions.size() > 0;
        if (trace) {
            printAllExceptions(decafScanner);
        }
    }

    public void printAllExceptions(DecafScanner decafScanner) {
        for (DecafSemanticException decafSemanticException : Visitor.exceptions) {
            TokenPosition tokenPosition = new TokenPosition(decafSemanticException.line, decafSemanticException.column, -1);
            DecafSemanticException decafSemanticException1 = new DecafSemanticException(tokenPosition, decafScanner.getContextualErrorMessage(tokenPosition, decafSemanticException.getMessage()));
            decafSemanticException1.setStackTrace(decafSemanticException.getStackTrace());
            decafSemanticException1.printStackTrace();
            //            System.err.println(decafSemanticException.getMessage());
        }
    }

    public void setTrace(boolean shouldTrace) {
        trace = shouldTrace;
    }

    public boolean hasError() {
        return hasError;
    }


}
