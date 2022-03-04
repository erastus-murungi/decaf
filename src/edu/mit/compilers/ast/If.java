package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class If extends Statement {
  final Expression test;
  final Block ifBlock;
  final Block elseBlock; // maybe null

  public If(TokenPosition tokenPosition, Expression test, Block ifBlock, Block elseBlock) {
    super(tokenPosition);
    this.test = test;
    this.ifBlock = ifBlock;
    this.elseBlock = elseBlock;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return (elseBlock != null)
        ? List.of(
            new Pair<>("test", test),
            new Pair<>("ifBody", ifBlock),
            new Pair<>("elseBody", elseBlock))
        : List.of(new Pair<>("test", test), new Pair<>("ifBody", ifBlock));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    if (elseBlock != null)
      return "If{" + "test=" + test + ", ifBlock=" + ifBlock + ", elseBlock=" + elseBlock + '}';
    else return "If{" + "test=" + test + ", ifBlock=" + ifBlock + '}';
  }

  @Override
  public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
    visitor.visit(this, curSymbolTable);
  }
}
