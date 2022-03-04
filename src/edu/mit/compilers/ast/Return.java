package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Return extends Statement {
  final Expression retExpression;

  public Return(TokenPosition tokenPosition, Expression expression) {
    super(tokenPosition);
    this.retExpression = expression;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return (retExpression == null) ? Collections.emptyList() : List.of(new Pair<>("return", retExpression));
  }

  @Override
  public boolean isTerminal() {
    return retExpression == null;
  }

  @Override
  public String toString() {
    return (retExpression == null) ? "Return{}" : "Return{" + "retExpression=" + retExpression + '}';
  }

  @Override
  public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
    visitor.visit(this, curSymbolTable);
  }
}
