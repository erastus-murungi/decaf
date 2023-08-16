package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

public class Assignment extends AST {
  private final String operator;
  public AssignExpr assignExpr;
  private final Location location;

  public Assignment(
      Location location,
      AssignExpr assignmentExpr,
      String operator
  ) {
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
  public Type getType() {
    return Type.Undefined;
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
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      SymbolTable currentSymbolTable
  ) {
    return ASTVisitor.visit(
        this,
        currentSymbolTable
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return codegenAstVisitor.visit(
        this,
        resultLocation
    );
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
