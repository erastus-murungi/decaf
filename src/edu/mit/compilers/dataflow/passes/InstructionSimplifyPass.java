package edu.mit.compilers.dataflow.passes;


import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.Location;
import edu.mit.compilers.ast.LocationArray;
import edu.mit.compilers.ast.LocationVariable;
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.BinaryOpExpression;
import edu.mit.compilers.ast.DecimalLiteral;
import edu.mit.compilers.ast.Expression;
import edu.mit.compilers.ast.HasExpression;
import edu.mit.compilers.ast.IntLiteral;

// This file implements routines for folding instructions into simpler forms
// that do not require creating new instructions.  This does constant folding
// ("add i32 1, 1" -> "2") but can also handle non-constant operands, either
// returning a constant ("and i32 %x, 0" -> "0") or an already existing value
// ("and i32 %x, %x" -> "%x").  All operands are assumed to have already been
// simplified: This is usually true and assuming it simplifies the logic (if
// they have not been simplified then results are correct but maybe suboptimal).

public class InstructionSimplifyPass {
    private static final IntLiteral mZero = new DecimalLiteral(null, "0");
    private static final IntLiteral mOne = new DecimalLiteral(null, "1");
    private static final BooleanLiteral mTrue = new BooleanLiteral(null, DecafScanner.RESERVED_TRUE);
    private static final BooleanLiteral mFalse = new BooleanLiteral(null, DecafScanner.RESERVED_FALSE);

    private static IntLiteral getZero(TokenPosition tokenPosition) {
        return new DecimalLiteral(tokenPosition, "0");
    }

    private static IntLiteral getOne(TokenPosition tokenPosition) {
        return new DecimalLiteral(tokenPosition, "1");
    }

    private static BooleanLiteral getTrue(TokenPosition tokenPosition) {
        return new BooleanLiteral(tokenPosition, DecafScanner.RESERVED_TRUE);
    }

    private static BooleanLiteral getFalse(TokenPosition tokenPosition) {
        return new BooleanLiteral(tokenPosition, DecafScanner.RESERVED_FALSE);
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
            hasExpression.compareAndSwapExpression(expression, newExpr);
            return expression != newExpr;
        }
        return false;
    }


    private static Expression trySimplifyEqInstruction(Expression expression) {
        if (expression instanceof BinaryOpExpression && ((BinaryOpExpression) expression).op.op.equals(DecafScanner.EQ)) {
            var eqInstruction = (BinaryOpExpression) expression;
            // X == true -> X
            // true == X -> X
            var X = eqInstruction.lhs;
            if (matchBinOpOperandsCommutative(eqInstruction, X, mTrue))
                return X;
            // true == true -> true
            if (matchBinOpOperands(eqInstruction, mTrue, mTrue)) {
                return getTrue(eqInstruction.tokenPosition);
            }
            // true == false -> false
            // false == true -> false
            if (matchBinOpOperandsCommutative(eqInstruction, mTrue, mFalse)) {
                return getFalse(expression.tokenPosition);
            }
        }
        return expression;
    }

    private static Expression trySimplifyNeqInstruction(Expression expression) {
        if (expression instanceof BinaryOpExpression && ((BinaryOpExpression) expression).op.op.equals(DecafScanner.NEQ)) {
            var eqInstruction = (BinaryOpExpression) expression;
            // X != false -> X
            // false != X -> X
            var X = eqInstruction.lhs;
            if (matchBinOpOperandsCommutative(eqInstruction, X, mFalse)) {
                return X;
            }
            // true != true -> false
            if (matchBinOpOperands(eqInstruction, mTrue, mTrue)) {
                return getFalse(eqInstruction.tokenPosition);
            }
            // true != false -> true
            // false != true -> true
            if (matchBinOpOperandsCommutative(eqInstruction, mTrue, mFalse)) {
                return getTrue(expression.tokenPosition);
            }
        }
        return expression;
    }

    private static Expression tryCollapseUnaryExpression(Expression expression) {
        if (expression instanceof UnaryOpExpression) {
            var unaryOpExpression = (UnaryOpExpression) expression;
            if (unaryOpExpression.op.op.equals(DecafScanner.MINUS)) {
                if (unaryOpExpression.operand instanceof IntLiteral) {
                    return new DecimalLiteral(unaryOpExpression.tokenPosition, "-" + unaryOpExpression.operand.getSourceCode());
                }
            } else if (unaryOpExpression.op.op.equals(DecafScanner.NOT)) {
                if (unaryOpExpression.operand instanceof BooleanLiteral) {
                    // !true = false
                    if (unaryOpExpression.operand.getSourceCode()
                            .equals(DecafScanner.RESERVED_FALSE)) {
                        return new BooleanLiteral(unaryOpExpression.tokenPosition, DecafScanner.RESERVED_TRUE);
                    }
                    // !false = true
                    if (unaryOpExpression.operand.getSourceCode()
                            .equals(DecafScanner.RESERVED_TRUE)) {
                        return new BooleanLiteral(unaryOpExpression.tokenPosition, DecafScanner.RESERVED_FALSE);
                    }
                }
            }
        }
        return expression;
    }

    private static boolean containsAlphabeticCharacters(String string) {
        return string.matches(".*[a-zA-Z]+.*");
    }

    private static Expression tryFoldConstantExpression(Expression expression) {
        // this check is necessary because the evaluator evaluates variables like 'e' and 'pi'
        if (containsAlphabeticCharacters(expression.getSourceCode())) {
            return expression;
        }
        var maybeEvaluatedLong = symbolicallyEvaluate(expression.getSourceCode());
        if (maybeEvaluatedLong != null)
            return new DecimalLiteral(expression.tokenPosition, maybeEvaluatedLong.toString());
        return expression;
    }

    public static boolean matchBinOpOperandsCommutative(BinaryOpExpression opExpression,
                                                        Expression lhsExpected,
                                                        Expression rhsExpected) {
        return matchBinOpOperands(opExpression, lhsExpected, rhsExpected) ||
                matchBinOpOperands(opExpression, rhsExpected, lhsExpected);
    }

    public static boolean matchBinOpOperands(BinaryOpExpression opExpression,
                                             Expression lhsExpected,
                                             Expression rhsExpected) {
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

    private static Long symbolicallyEvaluate(String string) {
        var expression = new com.udojava.evalex.Expression(string);
        try {
            var res = expression.eval();
            return res.longValue();
        } catch (Exception e) {
            return null;
        }
    }


    private static Expression trySimplifyAddInstruction(Expression expression) {
        if (expression instanceof BinaryOpExpression && ((BinaryOpExpression) expression).op.op.equals(DecafScanner.PLUS)) {
            var addInstruction = (BinaryOpExpression) expression;
            // 0 + X -> X
            var X = addInstruction.lhs;
            if (matchBinOpOperandsCommutative(addInstruction, mZero, X)) {
                return X;
            }
        }
        return expression;
    }

    private static Expression trySimplifyMulInstruction(Expression expression) {
        if (expression instanceof BinaryOpExpression && ((BinaryOpExpression) expression).op.op.equals(DecafScanner.MULTIPLY)) {
            var multiplyInstruction = (BinaryOpExpression) expression;
            // 0 * X -> 0
            // X * 0 -> 0
            var X = multiplyInstruction.lhs;
            if (matchBinOpOperandsCommutative(multiplyInstruction, mZero, X)) {
                return getZero(expression.tokenPosition);
            }
            // X * 1 -> X
            // 1 * X -> X
            if (matchBinOpOperandsCommutative(multiplyInstruction, mOne, X)) {
                return X;
            }
        }
        return expression;
    }

    private static Expression trySimplifySubInstruction(Expression expression) {
        if (expression instanceof BinaryOpExpression && ((BinaryOpExpression) expression).op.op.equals(DecafScanner.MINUS)) {
            var subInstruction = (BinaryOpExpression) expression;
            // X - X -> 0
            var X = subInstruction.lhs;
            if (matchBinOpOperands(subInstruction, X, X)) {
                return getZero(expression.tokenPosition);
            }

            // X - 0 -> X
            if (matchBinOpOperands(subInstruction, X, mZero)) {
                return X;
            }
        }
        return expression;
    }

    private static Expression trySimplifyDivInstruction(Expression expression) {
        if (expression instanceof BinaryOpExpression && ((BinaryOpExpression) expression).op.op.equals(DecafScanner.DIVIDE)) {
            var divInstruction = (BinaryOpExpression) expression;
            // X / X -> 1
            var X = divInstruction.lhs;
            if (matchBinOpOperands(divInstruction, X, X)) {
                return getOne(expression.tokenPosition);
            }

            // 0 / X -> 0
            if (matchBinOpOperands(divInstruction, mZero, X)) {
                return getOne(expression.tokenPosition);
            }

            // X / 1 -> X
            if (matchBinOpOperands(divInstruction, X, mOne)) {
                return X;
            }
        }
        return expression;
    }

    private static Expression trySimplifyModInstruction(Expression expression) {
        if (expression instanceof BinaryOpExpression && ((BinaryOpExpression) expression).op.op.equals(DecafScanner.MOD)) {
            var modInstruction = (BinaryOpExpression) expression;
            // 0 % X -> 0
            var X = modInstruction.rhs;
            if (matchBinOpOperands(modInstruction, mZero, X)) {
                return getZero(expression.tokenPosition);
            }
            // X % X -> 1
            if (matchBinOpOperands(modInstruction, X, X)) {
                return getOne(expression.tokenPosition);
            }
            // X % 1 -> 0
            if (matchBinOpOperands(modInstruction, X, mOne)) {
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
                    toReturn.add( ((HasExpression) astPair.second()));
                }
                toExplore.add(astPair.second());
            }
        }
        return toReturn;
    }

    public static void run(AST root) {
        getAllHasExpressionNodes(root).forEach(InstructionSimplifyPass::simplifyExpression);
    }
}
