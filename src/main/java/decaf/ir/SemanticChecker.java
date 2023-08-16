package decaf.ir;


import java.util.ArrayList;
import java.util.List;

import decaf.ast.AST;
import decaf.ast.Program;
import decaf.ast.Type;
import decaf.common.CompilationContext;
import decaf.descriptors.GlobalDescriptor;
import decaf.errors.SemanticError;


public class SemanticChecker {
  private final AST rootNode;
  private final CompilationContext context;
  private GlobalDescriptor globalDescriptor;
  private final List<SemanticError> errors = new ArrayList<>();

  public SemanticChecker(Program rootNode, CompilationContext context) {
    this.rootNode = rootNode;
    this.context = context;
    runChecks();
  }

  private void runChecks() {
    var genericSemanticChecker = new GenericSemanticChecker(errors);
    rootNode.accept(
        genericSemanticChecker,
        null
    );
    new TypeResolver(
        (Program) rootNode,
        genericSemanticChecker.getMethods(),
        genericSemanticChecker.getFields(),
        genericSemanticChecker.getImports(),
        errors
    );
    setGlobalDescriptor(new GlobalDescriptor(
        Type.Undefined,
        genericSemanticChecker.getFields(),
        genericSemanticChecker.getMethods(),
        genericSemanticChecker.getImports()
    ));
    if (context.debugModeOn()) {
      context.stringifyErrors(errors);
    }
  }

  public GlobalDescriptor getGlobalDescriptor() {
    return globalDescriptor;
  }

  public void setGlobalDescriptor(GlobalDescriptor globalDescriptor) {
    this.globalDescriptor = globalDescriptor;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public String getPrettyErrorOutput() {
    return context.stringifyErrors(errors);
  }
}
