package decaf.cfg;

import decaf.ast.ParenthesizedExpression;
import decaf.common.Utils;
import decaf.grammar.DecafScanner;
import decaf.ast.BinaryOpExpression;
import decaf.ast.ConditionalOperator;
import decaf.ast.Expression;
import decaf.ast.UnaryOpExpression;
import decaf.ast.UnaryOperator;

public class ShortCircuitProcessor {
    // to prevent object creation
    private ShortCircuitProcessor() {
    }

    public static BasicBlock shortCircuit(BasicBlock conditionalBlock) {
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
        if (expression instanceof final UnaryOpExpression unaryOpExpression) {
            if (unaryOpExpression.getUnaryOperator().label.equals(DecafScanner.NOT)) {
                final UnaryOperator unaryNot = unaryOpExpression.getUnaryOperator();
                final Expression operand = extractParenthesized(unaryOpExpression.operand);
                if (operand instanceof BinaryOpExpression binaryOpExpression) {
                    if (binaryOpExpression.op instanceof final ConditionalOperator operator) {
                        if (operator.label.equals(DecafScanner.CONDITIONAL_AND)) {

                            // Not (A and B) is the same as Not A or Not B.
                            return BinaryOpExpression.of(
                                    new UnaryOpExpression(unaryNot, simplify(binaryOpExpression.lhs)),
                                    new ConditionalOperator(binaryOpExpression.tokenPosition, DecafScanner.CONDITIONAL_OR),
                                    new UnaryOpExpression(unaryNot, simplify(binaryOpExpression.rhs)));
                        } else {
                            // Not (A or B) is the same as Not A and Not B.
                            return BinaryOpExpression.of(
                                    new UnaryOpExpression(unaryNot, simplify(binaryOpExpression.lhs)),
                                    new ConditionalOperator(binaryOpExpression.tokenPosition, DecafScanner.CONDITIONAL_AND),
                                    new UnaryOpExpression(unaryNot, simplify(binaryOpExpression.rhs)));
                        }
                    }
                }
            }
        }

        return expression;
    }

    private static BasicBlock shortCircuitImpl(BasicBlock basicBlockWithBranch) {
        final Expression expression = simplify(basicBlockWithBranch.getBranchCondition().orElseThrow());

        if (expression instanceof BinaryOpExpression conditional) {
            if (conditional.op instanceof ConditionalOperator operator) {

                basicBlockWithBranch.getFalseTarget()
                        .removePredecessor(basicBlockWithBranch);
                basicBlockWithBranch.getTrueTarget()
                        .removePredecessor(basicBlockWithBranch);

                final Expression c1 = Utils.rotateBinaryOpExpression(conditional.lhs);
                final Expression c2 = Utils.rotateBinaryOpExpression(conditional.rhs);

                BasicBlock b1, b2;

                if (operator.label.equals(DecafScanner.CONDITIONAL_AND)) {
                    b2 = shortCircuitImpl(BasicBlock.branch(c2, basicBlockWithBranch.getTrueTarget(), basicBlockWithBranch.getFalseTarget()));
                    b1 = shortCircuitImpl(BasicBlock.branch(c1, b2, basicBlockWithBranch.getFalseTarget()));
                } else {
                    b2 = shortCircuitImpl(BasicBlock.branch(c2, basicBlockWithBranch.getTrueTarget(), basicBlockWithBranch.getFalseTarget()));
                    b1 = shortCircuitImpl(BasicBlock.branch(c1, basicBlockWithBranch.getTrueTarget(), b2));
                }

                // TODO: improve the parent pointer logic here by removing checks
                if (basicBlockWithBranch.getTrueTarget()
                        .doesNotContainPredecessor(b1) && b1.getTrueTarget() == basicBlockWithBranch.getTrueTarget())
                    basicBlockWithBranch.getTrueTarget()
                            .addPredecessor(b1);
                if (basicBlockWithBranch.getFalseTarget()
                        .doesNotContainPredecessor(b1) && b1.getFalseTarget() == basicBlockWithBranch.getFalseTarget())
                    basicBlockWithBranch.getFalseTarget()
                            .addPredecessor(b1);
                if (basicBlockWithBranch.getTrueTarget()
                        .doesNotContainPredecessor(b2) && b2.getTrueTarget() == basicBlockWithBranch.getTrueTarget())
                    basicBlockWithBranch.getTrueTarget()
                            .addPredecessor(b2);
                if (basicBlockWithBranch.getFalseTarget()
                        .doesNotContainPredecessor(b2) && b2.getFalseTarget() == basicBlockWithBranch.getFalseTarget())
                    basicBlockWithBranch.getFalseTarget()
                            .addPredecessor(b2);
                if (b2.doesNotContainPredecessor(b1) && (b1.getFalseTarget() == b2 || b1.getTrueTarget() == b2))
                    b2.addPredecessor(b1);
                return b1;
            }
        }
        return basicBlockWithBranch;
    }
}