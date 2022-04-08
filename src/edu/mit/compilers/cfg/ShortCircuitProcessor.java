package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ast.ConditionalOperator;
import edu.mit.compilers.ast.UnaryOperator;

import static edu.mit.compilers.grammar.DecafScanner.*;

public class ShortCircuitProcessor {
    // to prevent object creation
    private ShortCircuitProcessor() {
    }

    public static CFGConditional shortCircuit(CFGConditional conditionalBlock) {
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
            if (unaryOpExpression.op.op.equals(NOT)) {
                final UnaryOperator unaryNot = unaryOpExpression.op;
                final Expression operand = extractParenthesized(unaryOpExpression.operand);
                if (operand instanceof BinaryOpExpression) {
                    BinaryOpExpression binaryOpExpression = (BinaryOpExpression) operand;
                    if (binaryOpExpression.op instanceof ConditionalOperator) {
                        final ConditionalOperator operator = (ConditionalOperator) binaryOpExpression.op;
                        if (operator.op.equals(CONDITIONAL_AND)) {

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

    private static CFGConditional shortCircuitImpl(CFGConditional cfgConditional) {
        Expression expression = simplify((Expression) cfgConditional.condition.ast);

        if (expression instanceof BinaryOpExpression) {
            BinaryOpExpression conditional = (BinaryOpExpression) expression;
            if (conditional.op instanceof ConditionalOperator) {
                ConditionalOperator operator = (ConditionalOperator) conditional.op;

                cfgConditional.falseChild.removePredecessor(cfgConditional);
                cfgConditional.trueChild.removePredecessor(cfgConditional);

                CFGExpression c1 = new CFGExpression(conditional.lhs);
                CFGExpression c2 = new CFGExpression(conditional.rhs);

                CFGConditional b1, b2;

                if (operator.op.equals(CONDITIONAL_AND)) {
                    b2 = shortCircuitImpl(new CFGConditional(c2, cfgConditional.trueChild, cfgConditional.falseChild));
                    b1 = shortCircuitImpl(new CFGConditional(c1, b2, cfgConditional.falseChild));
                } else {
                    b2 = shortCircuitImpl(new CFGConditional(c2, cfgConditional.trueChild, cfgConditional.falseChild));
                    b1 = shortCircuitImpl(new CFGConditional(c1, cfgConditional.trueChild, b2));
                }

                // TODO: improve the parent pointer logic here by removing checks
                if (!cfgConditional.trueChild.hasPredecessor(b1) && b1.trueChild == cfgConditional.trueChild)
                    cfgConditional.trueChild.addPredecessor(b1);
                if (!cfgConditional.falseChild.hasPredecessor(b1) && b1.falseChild == cfgConditional.falseChild)
                    cfgConditional.falseChild.addPredecessor(b1);
                if (!cfgConditional.trueChild.hasPredecessor(b2) && b2.trueChild == cfgConditional.trueChild)
                    cfgConditional.trueChild.addPredecessor(b2);
                if (!cfgConditional.falseChild.hasPredecessor(b2) && b2.falseChild == cfgConditional.falseChild)
                    cfgConditional.falseChild.addPredecessor(b2);
                if (!b2.hasPredecessor(b1) && (b1.falseChild == b2 || b1.trueChild == b2))
                    b2.addPredecessor(b1);
                return b1;
            }
        }
        return cfgConditional;
    }
}