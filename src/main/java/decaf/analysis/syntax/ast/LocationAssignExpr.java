package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class LocationAssignExpr extends Statement {
  public final Location location;
  public final AssignExpr assignExpr;

  public LocationAssignExpr(
      TokenPosition tokenPosition,
      Location location,
      AssignExpr assignExpr
  ) {
    super(tokenPosition);
    this.location = location;
    this.assignExpr = assignExpr;
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
  public String toString() {
    return "LocationAssignExpr{" + "location=" + location + ", assignExpr=" + assignExpr + '}';
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
  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return astVisitor.visit(
        this,
        input
    );
  }
}
