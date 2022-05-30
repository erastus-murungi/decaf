package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ast.ConditionalOperator;
import edu.mit.compilers.ast.UnaryOperator;

import static edu.mit.compilers.cfg.CFGVisitor.rotateBinaryOpExpression;
import static edu.mit.compilers.grammar.DecafScanner.*;

public class ShortCircuitProcessor {
    // to prevent object creation
    private ShortCircuitProcessor() {
    }

    public static BasicBlockWithBranch shortCircuit(BasicBlockWithBranch conditionalBlock) {
        return shortCircuitImpl(conditionalBlock);
    }

    public static Expression extractParenthesized(Expression expression) {
        if (expression instanceof ParenthesizedExpression)
            return extractParenthesized(((ParenthesizedExpression) expression).expression);
        return expression;
    }

    /**
     * This method trys to get rid of parentheses and applies De Morgan's law to compound expressions
     *
     * @param expression The expression to simplify
     * @return A simpler version of the expression if it exists
     */
    public static Expression simplify(Expression expression) {
        // Remove nested parenthesis
        expression = extractParenthesized(expression);

        // Apply De Morgan's Laws
        if (expression instanceof UnaryOpExpression) {
            final UnaryOpExpression unaryOpExpression = (UnaryOpExpression) expression;
            if (unaryOpExpression.getUnaryOperator().label.equals(NOT)) {
                final UnaryOperator unaryNot = unaryOpExpression.getUnaryOperator();
                final Expression operand = extractParenthesized(unaryOpExpression.operand);
                if (operand instanceof BinaryOpExpression) {
                    BinaryOpExpression binaryOpExpression = (BinaryOpExpression) operand;
                    if (binaryOpExpression.op instanceof ConditionalOperator) {
                        final ConditionalOperator operator = (ConditionalOperator) binaryOpExpression.op;
                        if (operator.label.equals(CONDITIONAL_AND)) {

                            // Not (A and B) is the same as Not A or Not B.
                            return BinaryOpExpression.of(
                                    new UnaryOpExpression(unaryNot, simplify(binaryOpExpression.lhs)),
                                    new ConditionalOperator(binaryOpExpression.tokenPosition, CONDITIONAL_OR),
                                    new UnaryOpExpression(unaryNot, simplify(binaryOpExpression.rhs)));
                        } else {
                            // Not (A or B) is the same as Not A and Not B.
                            return BinaryOpExpression.of(
                                    new UnaryOpExpression(unaryNot, simplify(binaryOpExpression.lhs)),
                                    new ConditionalOperator(binaryOpExpression.tokenPosition, CONDITIONAL_AND),
                                    new UnaryOpExpression(unaryNot, simplify(binaryOpExpression.rhs)));
                        }
                    }
                }
            }
        }

        return expression;
    }

    private static BasicBlockWithBranch shortCircuitImpl(BasicBlockWithBranch basicBlockWithBranch) {
        final Expression expression = simplify(basicBlockWithBranch.condition);

        if (expression instanceof BinaryOpExpression) {
            BinaryOpExpression conditional = (BinaryOpExpression) expression;
            if (conditional.op instanceof ConditionalOperator) {
                ConditionalOperator operator = (ConditionalOperator) conditional.op;

                basicBlockWithBranch.falseChild.removePredecessor(basicBlockWithBranch);
                basicBlockWithBranch.trueChild.removePredecessor(basicBlockWithBranch);

                final Expression c1 = rotateBinaryOpExpression(conditional.lhs);
                final Expression c2 = rotateBinaryOpExpression(conditional.rhs);

                BasicBlockWithBranch b1, b2;

                if (operator.label.equals(CONDITIONAL_AND)) {
                    b2 = shortCircuitImpl(new BasicBlockWithBranch(c2, basicBlockWithBranch.trueChild, basicBlockWithBranch.falseChild));
                    b1 = shortCircuitImpl(new BasicBlockWithBranch(c1, b2, basicBlockWithBranch.falseChild));
                } else {
                    b2 = shortCircuitImpl(new BasicBlockWithBranch(c2, basicBlockWithBranch.trueChild, basicBlockWithBranch.falseChild));
                    b1 = shortCircuitImpl(new BasicBlockWithBranch(c1, basicBlockWithBranch.trueChild, b2));
                }

                // TODO: improve the parent pointer logic here by removing checks
                if (basicBlockWithBranch.trueChild.doesNotContainPredecessor(b1) && b1.trueChild == basicBlockWithBranch.trueChild)
                    basicBlockWithBranch.trueChild.addPredecessor(b1);
                if (basicBlockWithBranch.falseChild.doesNotContainPredecessor(b1) && b1.falseChild == basicBlockWithBranch.falseChild)
                    basicBlockWithBranch.falseChild.addPredecessor(b1);
                if (basicBlockWithBranch.trueChild.doesNotContainPredecessor(b2) && b2.trueChild == basicBlockWithBranch.trueChild)
                    basicBlockWithBranch.trueChild.addPredecessor(b2);
                if (basicBlockWithBranch.falseChild.doesNotContainPredecessor(b2) && b2.falseChild == basicBlockWithBranch.falseChild)
                    basicBlockWithBranch.falseChild.addPredecessor(b2);
                if (b2.doesNotContainPredecessor(b1) && (b1.falseChild == b2 || b1.trueChild == b2))
                    b2.addPredecessor(b1);
                return b1;
            }
        }
        return basicBlockWithBranch;
    }
}