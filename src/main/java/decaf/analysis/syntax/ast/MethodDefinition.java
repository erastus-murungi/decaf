package decaf.analysis.syntax.ast;


import java.util.ArrayList;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class MethodDefinition extends AST {
  private final TokenPosition tokenPosition;
  private final Type returnType;
  private final RValue methodAstName;
  private final FormalArguments formalArguments;
  private final Block block;

  public MethodDefinition(
      TokenPosition tokenPosition,
      Type returnType,
      FormalArguments formalArguments,
      RValue methodAstName,
      Block block
  ) {
    this.tokenPosition = tokenPosition;
    this.returnType = returnType;
    this.formalArguments = formalArguments;
    this.methodAstName = methodAstName;
    this.block = block;
  }

  public String getName() {
    return methodAstName.getLabel();
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    List<Pair<String, AST>> nodes = new ArrayList<>();
    nodes.add(new Pair<>(
        "returnType",
        new RValue(
            getReturnType().toString(),
            getTokenPosition()
        )
    ));
    nodes.add(new Pair<>(
        "methodName",
        methodAstName
    ));
    for (FormalArgument formalArgument : getFormalArguments()) {
      nodes.add(new Pair<>(
          "arg",
          formalArgument
      ));
    }
    nodes.add(new Pair<>(
        "block",
        getBody()
    ));

    return nodes;
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "MethodDefinition{" +
           ", returnType=" + getReturnType() +
           ", methodName=" + getName() +
           ", parameterList=" + getFormalArguments() +
           ", block=" + getBody() +
           '}';
  }

  @Override
  public String getSourceCode() {
    List<String> params = new ArrayList<>();
    for (FormalArgument formalArgument : getFormalArguments()) {
      String sourceCode = formalArgument.getSourceCode();
      params.add(sourceCode);
    }
    String indent = " ".repeat(getReturnType().getSourceCode()
                                              .length() + getName()
                                                                   .length() + 2);
    return String.format("%s %s(%s) {\n    %s\n}",
                         getReturnType().getSourceCode(),
                         getName(),
                         String.join(
                             ",\n" + indent,
                             params
                         )
        ,
                         getBody().getSourceCode()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
    );
  }

  public TokenPosition getTokenPosition() {
    return tokenPosition;
  }

  public Type getReturnType() {
    return returnType;
  }

  public FormalArguments getFormalArguments() {
    return formalArguments;
  }

  public FormalArgument get(int index) {
    return formalArguments.get(index);
  }

  public Block getBody() {
    return block;
  }

  public boolean hasNoFormalArguments() {
    return formalArguments.iterator().hasNext() == false;
  }
}
