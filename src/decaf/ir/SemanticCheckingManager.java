package decaf.ir;

import decaf.ast.Type;
import decaf.common.DecafExceptionProcessor;
import decaf.ast.AST;
import decaf.ast.Program;
import decaf.descriptors.GlobalDescriptor;
import decaf.exceptions.DecafSemanticException;


public class SemanticCheckingManager {
    private final AST rootNode;
    private GlobalDescriptor globalDescriptor;
    private boolean trace;
    private boolean hasError;

    public SemanticCheckingManager(Program rootNode) {
        this.rootNode = rootNode;
    }

    public void runChecks(DecafExceptionProcessor decafExceptionProcessor) {
        var semanticCheckerVisitor = new GenericSemanticChecker();
        rootNode.accept(semanticCheckerVisitor, null);
        var typeCheckVisitor = new TypeResolver((Program) rootNode,
                                                semanticCheckerVisitor.getMethods(),
                                                semanticCheckerVisitor.getFields(),
                                                semanticCheckerVisitor.getImports()
        );
        rootNode.accept(typeCheckVisitor,
                        semanticCheckerVisitor.getFields()
        );
        setGlobalDescriptor(new GlobalDescriptor(
            Type.Undefined,
            semanticCheckerVisitor.getFields(),
            semanticCheckerVisitor.getMethods(),
            semanticCheckerVisitor.getImports()
        ));
        hasError = AstVisitor.exceptions.size() > 0;
        if (trace) {
            printAllExceptions(decafExceptionProcessor);
        }
    }

    public void printAllExceptions(DecafExceptionProcessor decafExceptionProcessor) {
        for (DecafSemanticException decafSemanticException : AstVisitor.exceptions) {
            decafExceptionProcessor.processDecafSemanticException(decafSemanticException).printStackTrace();
        }
    }

    public void setTrace(boolean shouldTrace) {
        trace = shouldTrace;
    }

    public boolean hasError() {
        return hasError;
    }


    public GlobalDescriptor getGlobalDescriptor() {
        return globalDescriptor;
    }

    public void setGlobalDescriptor(GlobalDescriptor globalDescriptor) {
        this.globalDescriptor = globalDescriptor;
    }
}
