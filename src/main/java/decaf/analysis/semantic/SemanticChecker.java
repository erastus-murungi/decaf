package decaf.analysis.semantic;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Program;
import decaf.analysis.syntax.ast.Type;
import decaf.shared.CompilationContext;
import decaf.shared.descriptors.GlobalDescriptor;
import decaf.shared.errors.SemanticError;
import java.util.ArrayList;
import java.util.List;

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
    rootNode.accept(genericSemanticChecker, null);
    new TypeResolver((Program)rootNode, genericSemanticChecker.getMethods(),
                     genericSemanticChecker.getFields(),
                     genericSemanticChecker.getImports(), errors);
    setGlobalDescriptor(
        new GlobalDescriptor(Type.Undefined, genericSemanticChecker.getFields(),
                             genericSemanticChecker.getMethods(),
                             genericSemanticChecker.getImports()));
    if (context.debugModeOn()) {
      context.stringifyErrors(errors);
    }
  }

  public GlobalDescriptor getGlobalDescriptor() { return globalDescriptor; }

  public void setGlobalDescriptor(GlobalDescriptor globalDescriptor) {
    this.globalDescriptor = globalDescriptor;
  }

  public boolean hasErrors() { return !errors.isEmpty(); }

  public String getPrettyErrorOutput() {
    return context.stringifyErrors(errors);
  }
}
