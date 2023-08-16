package decaf.ir.dataflow.passes;


import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.BinaryOpExpression;
import decaf.analysis.syntax.ast.BooleanLiteral;
import decaf.analysis.syntax.ast.DecimalLiteral;
import decaf.analysis.syntax.ast.Expression;
import decaf.analysis.syntax.ast.HasExpression;
import decaf.analysis.syntax.ast.IntLiteral;
import decaf.analysis.syntax.ast.Literal;
import decaf.analysis.syntax.ast.LocationArray;
import decaf.analysis.syntax.ast.UnaryOpExpression;
import decaf.shared.Utils;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.TokenPosition;

// This file implements routines for folding instructions into simpler forms
// that do not require creating new instructions.  This does constant folding
// ("add i32 1, 1" -> "2") but can also handle non-constant operands, either
// returning a constant ("and i32 %x, 0" -> "0") or an already existing value
// ("and i32 %x, %x" -> "%x").  All operands are assumed to have already been
// simplified: This is usually true and assuming it simplifies the logic (if
// they have not been simplified then results are correct but maybe suboptimal).

public class InstructionSimplifyIrPass {
  private static final IntLiteral mZero = new DecimalLiteral(
      null,
      "0"
  );
  private static final IntLiteral mOne = new DecimalLiteral(
      null,
      "1"
  );
  private static final BooleanLiteral mTrue = new BooleanLiteral(
      null,
      Scanner.RESERVED_TRUE
  );
  private static final BooleanLiteral mFalse = new BooleanLiteral(
      null,
      Scanner.RESERVED_FALSE
  );

  private static IntLiteral getZero(TokenPosition tokenPosition) {
    return new DecimalLiteral(
        tokenPosition,
        "0"
    );
  }

  private static IntLiteral getOne(TokenPosition tokenPosition) {
    return new DecimalLiteral(
        tokenPosition,
        "1"
    );
  }

  private static BooleanLiteral getTrue(TokenPosition tokenPosition) {
    return new BooleanLiteral(
        tokenPosition,
        Scanner.RESERVED_TRUE
    );
  }

  private static BooleanLiteral getFalse(TokenPosition tokenPosition) {
    return new BooleanLiteral(
        tokenPosition,
        Scanner.RESERVED_FALSE
    );
  }

  public static boolean simplifyExpression(HasExpression hasExpression) {
    for (var expression : hasExpression.getExpression()) {
      var newExpr = expression;
      newExpr = tryFoldConstantExpression(newExpr);
      newExpr = trySimplifyAddInstruction(newExpr);
      newExpr = trySimplifyMulInstruction(newExpr);
      newExpr = trySimplifySubInstruction(newExpr);
      newExpr = trySimplifyDivInstruction(newExpr);
      newExpr = trySimplifyModInstruction(newExpr);
      newExpr = trySimplifyNeqInstruction(newExpr);
      newExpr = trySimplifyEqInstruction(newExpr);
      newExpr = tryCollapseUnaryExpression(newExpr);
      hasExpression.compareAndSwapExpression(
          expression,
          newExpr
      );
      return expression != newExpr;
    }
    return false;
  }


  private static Expression trySimplifyEqInstruction(Expression expression) {
    if (expression instanceof BinaryOpExpression eqInstruction &&
        ((BinaryOpExpression) expression).op.label.equals(Scanner.EQ)) {
      // X == true -> X
      // true == X -> X
      var X = eqInstruction.lhs;
      if (matchBinOpOperandsCommutative(
          eqInstruction,
          X,
          mTrue
      ))
        return X;
      // true == true -> true
      if (matchBinOpOperands(
          eqInstruction,
          mTrue,
          mTrue
      )) {
        return getTrue(eqInstruction.tokenPosition);
      }
      // true == false -> false
      // false == true -> false
      if (matchBinOpOperandsCommutative(
          eqInstruction,
          mTrue,
          mFalse
      )) {
        return getFalse(expression.tokenPosition);
      }
    }
    return expression;
  }

  private static Expression trySimplifyNeqInstruction(Expression expression) {
    if (expression instanceof BinaryOpExpression eqInstruction &&
        ((BinaryOpExpression) expression).op.label.equals(Scanner.NEQ)) {
      // X != false -> X
      // false != X -> X
      var X = eqInstruction.lhs;
      if (matchBinOpOperandsCommutative(
          eqInstruction,
          X,
          mFalse
      )) {
        return X;
      }
      // true != true -> false
      if (matchBinOpOperands(
          eqInstruction,
          mTrue,
          mTrue
      )) {
        return getFalse(eqInstruction.tokenPosition);
      }
      // true != false -> true
      // false != true -> true
      if (matchBinOpOperandsCommutative(
          eqInstruction,
          mTrue,
          mFalse
      )) {
        return getTrue(expression.tokenPosition);
      }
    }
    return expression;
  }

  private static Expression tryCollapseUnaryExpression(Expression expression) {
    if (expression instanceof UnaryOpExpression unaryOpExpression) {
      if (unaryOpExpression.getUnaryOperator().label.equals(Scanner.MINUS)) {
        if (unaryOpExpression.operand instanceof IntLiteral) {
          return new DecimalLiteral(
              unaryOpExpression.tokenPosition,
              "-" + unaryOpExpression.operand.getSourceCode()
          );
        }
      } else if (unaryOpExpression.getUnaryOperator().label.equals(Scanner.NOT)) {
        if (unaryOpExpression.operand instanceof BooleanLiteral) {
          // !true = false
          if (unaryOpExpression.operand.getSourceCode()
                                       .equals(Scanner.RESERVED_FALSE)) {
            return new BooleanLiteral(
                unaryOpExpression.tokenPosition,
                Scanner.RESERVED_TRUE
            );
          }
          // !false = true
          if (unaryOpExpression.operand.getSourceCode()
                                       .equals(Scanner.RESERVED_TRUE)) {
            return new BooleanLiteral(
                unaryOpExpression.tokenPosition,
                Scanner.RESERVED_FALSE
            );
          }
        }
      }
    }
    return expression;
  }

  public static Expression tryFoldConstantExpression(Expression expression) {
    if (expression instanceof Literal)
      return expression;
    var maybeEvaluatedLong = Utils.symbolicallyEvaluate(expression.getSourceCode());
    if (maybeEvaluatedLong.isPresent())
      return new DecimalLiteral(
          expression.tokenPosition,
          maybeEvaluatedLong.get()
                            .toString()
      );
    return expression;
  }

  private static Expression getNotEqTo(
      BinaryOpExpression binaryOpExpression,
      String expected
  ) {
    if (binaryOpExpression.lhs.getSourceCode()
                              .equals(expected))
      return binaryOpExpression.rhs;
    return binaryOpExpression.lhs;
  }

  private static Expression getNonZero(BinaryOpExpression binaryOpExpression) {
    return getNotEqTo(
        binaryOpExpression,
        mZero.getSourceCode()
    );
  }

  private static Expression getNotOne(BinaryOpExpression binaryOpExpression) {
    return getNotEqTo(
        binaryOpExpression,
        mOne.getSourceCode()
    );
  }

  public static boolean matchBinOpOperandsCommutative(
      BinaryOpExpression opExpression,
      Expression lhsExpected,
      Expression rhsExpected
  ) {
    return matchBinOpOperands(
        opExpression,
        lhsExpected,
        rhsExpected
    ) ||
        matchBinOpOperands(
            opExpression,
            rhsExpected,
            lhsExpected
        );
  }

  public static boolean matchBinOpOperands(
      BinaryOpExpression opExpression,
      Expression lhsExpected,
      Expression rhsExpected
  ) {
    var lhsActual = opExpression.lhs;
    var rhsActual = opExpression.rhs;
    if (lhsActual instanceof LocationArray && rhsActual instanceof LocationArray) {
      // we add this extra check because array locations maybe indexed by global values which may change state
      // for instance a[foo()] - a[foo()] is not necessarily 0, because the return of foo(0) could be some
      // changing state between every invocation
      return false;
    }
    return lhsActual.getSourceCode()
                    .equals(lhsExpected.getSourceCode()) &&
        rhsActual.getSourceCode()
                 .equals(rhsExpected.getSourceCode());
  }


  private static Expression trySimplifyAddInstruction(Expression expression) {
    if (expression instanceof BinaryOpExpression addInstruction &&
        ((BinaryOpExpression) expression).op.label.equals(Scanner.PLUS)) {
      // 0 + X -> X
      var X = addInstruction.lhs;
      if (matchBinOpOperandsCommutative(
          addInstruction,
          mZero,
          X
      )) {
        return X;
      }
    }
    return expression;
  }

  private static Expression trySimplifyMulInstruction(Expression expression) {
    if (expression instanceof BinaryOpExpression multiplyInstruction &&
        ((BinaryOpExpression) expression).op.label.equals(Scanner.MULTIPLY)) {
      // 0 * X -> 0
      // X * 0 -> 0
      var X = multiplyInstruction.lhs;
      if (matchBinOpOperandsCommutative(
          multiplyInstruction,
          mZero,
          X
      )) {
        return getZero(expression.tokenPosition);
      }
      // X * 1 -> X
      // 1 * X -> X
      if (matchBinOpOperandsCommutative(
          multiplyInstruction,
          mOne,
          X
      )) {
        return X;
      }
    }
    return expression;
  }

  private static Expression trySimplifySubInstruction(Expression expression) {
    if (expression instanceof BinaryOpExpression subInstruction &&
        ((BinaryOpExpression) expression).op.label.equals(Scanner.MINUS)) {
      // X - X -> 0
      var X = subInstruction.lhs;
      if (matchBinOpOperands(
          subInstruction,
          X,
          X
      )) {
        return getZero(expression.tokenPosition);
      }

      // X - 0 -> X
      if (matchBinOpOperands(
          subInstruction,
          X,
          mZero
      )) {
        return X;
      }
    }
    return expression;
  }

  private static Expression trySimplifyDivInstruction(Expression expression) {
    if (expression instanceof BinaryOpExpression divInstruction &&
        ((BinaryOpExpression) expression).op.label.equals(Scanner.DIVIDE)) {
      // X / X -> 1
      var X = divInstruction.lhs;
      if (matchBinOpOperands(
          divInstruction,
          X,
          X
      )) {
        return getOne(expression.tokenPosition);
      }

      // 0 / X -> 0
      if (matchBinOpOperands(
          divInstruction,
          mZero,
          X
      )) {
        return getOne(expression.tokenPosition);
      }

      // X / 1 -> X
      if (matchBinOpOperands(
          divInstruction,
          X,
          mOne
      )) {
        return X;
      }
    }
    return expression;
  }

  private static Expression trySimplifyModInstruction(Expression expression) {
    if (expression instanceof BinaryOpExpression modInstruction &&
        ((BinaryOpExpression) expression).op.label.equals(Scanner.MOD)) {
      // 0 % X -> 0
      var X = modInstruction.rhs;
      if (matchBinOpOperands(
          modInstruction,
          mZero,
          X
      )) {
        return getZero(expression.tokenPosition);
      }
      // X % X -> 1
      if (matchBinOpOperands(
          modInstruction,
          X,
          X
      )) {
        return getOne(expression.tokenPosition);
      }
      // X % 1 -> 0
      if (matchBinOpOperands(
          modInstruction,
          X,
          mOne
      )) {
        return getZero(expression.tokenPosition);
      }
    }
    return expression;
  }

  private static Set<HasExpression> getAllHasExpressionNodes(AST root) {
    var toReturn = new HashSet<HasExpression>();
    var toExplore = new Stack<AST>();
    toExplore.push(root);
    while (!toExplore.isEmpty()) {
      var ast = toExplore.pop();
      for (var astPair : ast.getChildren()) {
        if (astPair.second() instanceof HasExpression) {
          toReturn.add(((HasExpression) astPair.second()));
        }
        toExplore.add(astPair.second());
      }
    }
    return toReturn;
  }

  public static void run(AST root) {
    getAllHasExpressionNodes(root).forEach(InstructionSimplifyIrPass::simplifyExpression);
  }
}