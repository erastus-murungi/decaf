package decaf.analysis.syntax.ast.types;

import decaf.analysis.syntax.ast.AST;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Type extends AST {
  private final TypeId typeId;
  public enum TypeId {
    // primitives
    Int,
    Bool,
    Void,
    String,
    // unset
    Unset,
    // derived
    Array
  }

  public boolean isPrimitiveType() {
    return typeId == TypeId.Int || typeId == TypeId.Bool || typeId == TypeId.Void || typeId == TypeId.String;
  }

  public boolean isDerivedArrayType() {
    return typeId == TypeId.Array;
  }

  private static final Type intType = new Type(TypeId.Int);
  private static final Type boolType = new Type(TypeId.Bool);
  private static final Type voidType = new Type(TypeId.Void);
  private static final Type stringType = new Type(TypeId.String);
  private static final Type unsetType = new Type(TypeId.Unset);

  public static Type getIntType() {
    return intType;
  }

  public static Type getBoolType() {
    return boolType;
  }

  public static Type getVoidType() {
    return voidType;
  }

  public static Type getStringType() {
    return stringType;
  }

  public static Type getUnsetType() {
    return unsetType;
  }

  protected Type(@NotNull TypeId typeId) {
    this.typeId = typeId;
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
  public <T> T accept(@NotNull AstVisitor<T> astVisitor, @NotNull Scope currentScope) {
    return astVisitor.visit(this, currentScope);
  }

  @Override
  public String getSourceCode() {
    return null;
  }

  @Override
  public String toString() {
    return switch (typeId) {
      case Int -> "int";
      case Bool -> "bool";
      case Void -> "void";
      case String -> "string";
      case Unset -> "unset";
      default -> throw new IllegalStateException("Unexpected value: " + typeId);
    };
  }
}
