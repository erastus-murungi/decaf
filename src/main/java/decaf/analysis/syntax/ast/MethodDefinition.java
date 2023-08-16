package decaf.analysis.syntax.ast;


import java.util.ArrayList;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class MethodDefinition extends AST {
  private final TokenPosition tokenPosition;
  private final Type returnType;
  private final RValue methodRValue;
  private final List<FormalArgument> formalArguments;
  private final Block block;

  public MethodDefinition(
      TokenPosition tokenPosition,
      Type returnType,
      List<FormalArgument> formalArguments,
      RValue methodRValue,
      Block block
  ) {
    this.tokenPosition = tokenPosition;
    this.returnType = returnType;
    this.formalArguments = formalArguments;
    this.methodRValue = methodRValue;
    this.block = block;
  }

  @Override
  public Type getType() {
    return Type.Undefined;
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
        getMethodName()
    ));
    for (FormalArgument formalArgument : getFormalArguments()) {
      nodes.add(new Pair<>(
          "arg",
          formalArgument
      ));
    }
    nodes.add(new Pair<>(
        "block",
        getBlock()
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
        ", methodName=" + getMethodName() +
        ", parameterList=" + getFormalArguments() +
        ", block=" + getBlock() +
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
                                              .length() + getMethodName().getSourceCode()
                                                                         .length() + 2);
    return String.format("%s %s(%s) {\n    %s\n}",
                         getReturnType().getSourceCode(),
                         getMethodName().getSourceCode(),
                         String.join(
                             ",\n" + indent,
                             params
                         )
        ,
                         getBlock().getSourceCode()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope curScope
  ) {
    return ASTVisitor.visit(
        this,
        curScope
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }

  public TokenPosition getTokenPosition() {
    return tokenPosition;
  }

  public Type getReturnType() {
    return returnType;
  }

  public RValue getMethodName() {
    return methodRValue;
  }

  public List<FormalArgument> getFormalArguments() {
    return formalArguments;
  }

  public Block getBlock() {
    return block;
  }
}
