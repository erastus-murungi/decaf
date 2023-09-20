package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Decrement extends AssignExpr {
  public Decrement(TokenPosition tokenPosition) {
    super(
        tokenPosition,
        null
    );
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public String toString() {
    return "Decrement{}";
  }

  @Override
  public String getSourceCode() {
    return Scanner.DECREMENT;
  }

  @Override
  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return astVisitor.visit(
        this,
        input
    );
  }

  @Override
  public String getOperator() {
    return "--";
  }
}
