package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Assignment extends Statement {
  private final String operator;
  private final Location location;
  public AssignExpr assignExpr;

  public Assignment(
      TokenPosition tokenPosition,
      Location location,
      AssignExpr assignmentExpr,
      String operator
  ) {
      super(tokenPosition);
      this.location = location;
    this.assignExpr = assignmentExpr;
    this.operator = operator;
  }

  public Location getLocation() {
    return location;
  }

  public String getOperator() {
    return operator;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "location",
            location
        ),
        new Pair<>(
            "assignExpr",
            assignExpr
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public <ReturnType, InputType> ReturnType accept(AstVisitor<ReturnType, InputType> astVisitor, InputType input) {
    return astVisitor.visit(this, input);
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s",
        location.getSourceCode(),
        assignExpr.getSourceCode()
    );
  }

  @Override
  public String toString() {
    return "Assignment{" +
        "location=" + location +
        ", assignExpr=" + assignExpr +
        '}';
  }
}
