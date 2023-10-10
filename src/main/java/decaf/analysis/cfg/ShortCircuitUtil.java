package decaf.analysis.cfg;

import decaf.analysis.lexical.Scanner;
import decaf.analysis.syntax.ast.*;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkState;

public class ShortCircuitUtil {
    // to prevent object creation
    private ShortCircuitUtil() {
    }

    public static void shortCircuit(@NotNull CfgBlock conditionalBlock) {
        shortCircuitImpl(conditionalBlock);
    }

    public static @NotNull Expression extractParenthesized(@NotNull Expression expression) {
        if (expression instanceof ParenthesizedExpression parenthesizedExpression) {
            return extractParenthesized(parenthesizedExpression.getExpression());
        }
        return expression;
    }

    /**
     * This method trys to get rid of parentheses and applies De Morgan's law to compound expressions
     *
     * @param expression The expression to simplify
     * @return A simpler version of the expression if it exists
     */
    public static @NotNull Expression applyDeMorgansLaw(@NotNull Expression expression) {
        // Remove nested parenthesis
        expression = extractParenthesized(expression);

        // Apply De Morgan's Laws
        if (expression instanceof final UnaryOpExpression unaryOpExpression) {
            if (unaryOpExpression.getUnaryOperator().getLabel().equals(Scanner.NOT)) {
                final var unaryNot = unaryOpExpression.getUnaryOperator();
                final var operand = extractParenthesized(unaryOpExpression.operand);
                if (operand instanceof BinaryOpExpression binaryOpExpression) {
                    if (binaryOpExpression.getOp() instanceof final ConditionalOperator operator) {
                        if (operator.getLabel().equals(Scanner.CONDITIONAL_AND)) {

                            // Not (A and B) is the same as Not A or Not B.
                            return BinaryOpExpression.of(new UnaryOpExpression(unaryNot,
                                                                               applyDeMorgansLaw(binaryOpExpression.getLhs())
                                                         ),
                                                         new ConditionalOperator(binaryOpExpression.getTokenPosition(),
                                                                                 Scanner.CONDITIONAL_OR
                                                         ),
                                                         new UnaryOpExpression(unaryNot,
                                                                               applyDeMorgansLaw(binaryOpExpression.getRhs())
                                                         )
                                                        );
                        } else {
                            // Not (A or B) is the same as Not A and Not B.
                            return BinaryOpExpression.of(new UnaryOpExpression(unaryNot,
                                                                               applyDeMorgansLaw(binaryOpExpression.getLhs())
                                                         ),
                                                         new ConditionalOperator(binaryOpExpression.getTokenPosition(),
                                                                                 Scanner.CONDITIONAL_AND
                                                         ),
                                                         new UnaryOpExpression(unaryNot,
                                                                               applyDeMorgansLaw(binaryOpExpression.getRhs())
                                                         )
                                                        );
                        }
                    }
                }
            }
        }

        return expression;
    }

    private static CfgBlock shortCircuitImpl(@NotNull CfgBlock cfgBlockWithBranch) {
        checkState(cfgBlockWithBranch.hasBranch(), "expected a branching block");
        final var expression = applyDeMorgansLaw(cfgBlockWithBranch.getBranchCondition());

        if (expression instanceof BinaryOpExpression condition) {
            if (condition.getOp() instanceof ConditionalOperator conditionalOperator) {

                final var successor = cfgBlockWithBranch.getSuccessorOrThrow();
                final var alternateSuccessor = cfgBlockWithBranch.getAlternateSuccessorOrThrow();

                cfgBlockWithBranch.unlinkFromSuccessor(successor);
                cfgBlockWithBranch.unlinkFromSuccessor(alternateSuccessor);

                final var c1 = condition.getLhs();
                final var c2 = condition.getRhs();

                CfgBlock b1, b2;

                if (conditionalOperator.getLabel().equals(Scanner.CONDITIONAL_AND)) {
                    b2 = shortCircuitImpl(CfgBlock.withBranch(c2.toEvalCondition(), successor, alternateSuccessor));
                    b1 = shortCircuitImpl(CfgBlock.withBranch(c1.toEvalCondition(), b2, alternateSuccessor));
                } else {
                    checkState(conditionalOperator.getLabel().equals(Scanner.CONDITIONAL_OR),
                               "expected an OR operator"
                              );
                    b2 = shortCircuitImpl(CfgBlock.withBranch(c2.toEvalCondition(), successor, alternateSuccessor));
                    b1 = shortCircuitImpl(CfgBlock.withBranch(c1.toEvalCondition(), successor, b2));
                }
                cfgBlockWithBranch.replaceWith(b1);
            }
        }
        return cfgBlockWithBranch;
    }
}