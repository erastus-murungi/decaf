package decaf.analysis.semantic;

import java.util.ArrayList;
import java.util.List;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Program;
import decaf.analysis.syntax.ast.Type;
import decaf.shared.CompilationContext;
import decaf.shared.descriptors.GlobalDescriptor;
import decaf.shared.errors.SemanticError;

public class SemanticChecker {
  private final AST rootNode;
  private final CompilationContext context;
  private final List<SemanticError> errors = new ArrayList<>();
  private GlobalDescriptor globalDescriptor;

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
    new TypeChecker((Program) rootNode,
                    genericSemanticChecker.getMethods(),
                    genericSemanticChecker.getFields(),
                    genericSemanticChecker.getImports(),
                    errors
    );
    setGlobalDescriptor(
        new GlobalDescriptor(Type.Undefined,
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
