package decaf.ir.cfg;

import org.jetbrains.annotations.NotNull;

import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.analysis.syntax.ast.Program;
import decaf.shared.CompilationContext;

public class CFG {
  @NotNull
  private final CompilationContext context;

  public CFG(@NotNull CompilationContext context) {
    this.context = context;
  }

  private void build(@NotNull Program program) {
  }



  private void buildCfgForMethod(@NotNull MethodDefinition methodDefinition) {
    var basicBlock = BasicBlock.noBranch();
    basicBlock.addAstNodes(methodDefinition.getBody().getFieldDeclarations());
    for (var statement : methodDefinition.getBody().getStatements()) {
    }
  }
}
