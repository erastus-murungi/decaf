package decaf.analysis.syntax.ast;


import java.util.ArrayList;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Block extends AST {
  final private List<FieldDeclaration> fieldDeclarationList;
  final private List<Statement> statementList;
  public Scope scope;

  public Block(
      TokenPosition tokenPosition,
      List<FieldDeclaration> fieldDeclarationList,
      List<Statement> statementList
  ) {
    super(tokenPosition);
    this.fieldDeclarationList = fieldDeclarationList;
    this.statementList = statementList;
  }

  public List<Statement> getStatements() {
    return statementList;
  }

  public List<FieldDeclaration> getFieldDeclarations() {
    return fieldDeclarationList;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    ArrayList<Pair<String, AST>> nodeArrayList = new ArrayList<>();
    for (FieldDeclaration fieldDeclaration : fieldDeclarationList)
      nodeArrayList.add(new Pair<>(
          "fieldDeclaration",
          fieldDeclaration
      ));
    for (Statement statement : statementList)
      nodeArrayList.add(new Pair<>(
          "statement",
          statement
      ));
    return nodeArrayList;
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "Block{" + "fieldDeclarationList=" + fieldDeclarationList + ", statementList=" + statementList + '}';
  }

  @Override
  public String getSourceCode() {
    List<String> list = new ArrayList<>();
    for (FieldDeclaration fieldDeclaration : fieldDeclarationList) {
      String sourceCode = fieldDeclaration.getSourceCode();
      list.add(sourceCode);
    }
    String fieldDeclarations = String.join(
        ";\n    ",
        list
    );
    List<String> s = new ArrayList<>();
    for (int i = 0; i < statementList.size(); i++) {
      Statement statement = statementList.get(i);
      s.add(statement.getSourceCode());
      if (statement instanceof For || statement instanceof While || statement instanceof If) {
        s.add((i == statementList.size() - 1) ? "    ": "\n    ");
      } else {
        s.add((i == statementList.size() - 1) ? ";    ": ";\n    ");
      }
    }
    return fieldDeclarations + (fieldDeclarations.isBlank() ? "": ";\n    ") + String.join(
        "",
        s
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