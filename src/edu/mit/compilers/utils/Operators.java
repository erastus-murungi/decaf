package edu.mit.compilers.utils;

public class Operators {
    public static final String PLUS = "+";
    public static final String MINUS = "-";
    public static final String DIVIDE = "/";
    public static final String MULTIPLY = "*";
    public static final String MOD = "%";

    public static final String LT = "<";
    public static final String GT = ">";

    public static final String LEQ = "<=";
    public static final String GEQ = ">=";
    public static final String EQ = "==";
    public static final String NEQ = "!=";

    public static final String CONDITIONAL_OR = "||";
    public static final String CONDITIONAL_AND = "&&";

    public static final String ADD_ASSIGN = "+=";
    public static final String MINUS_ASSIGN = "-=";
    public static final String MULTIPLY_ASSIGN = "*=";

    public static final String INCREMENT = "++";
    public static final String DECREMENT = "--";

    public static final String ASSIGN = "=";

    public static final String NOT = "!";

    public static String getOperatorName(String operator) {
        switch (operator) {
            case PLUS:
                return "add";
            case MINUS:
                return "sub";
            case DIVIDE:
                return "idiv";
            case MULTIPLY:
                return "mul";
            case MOD:
                return "mod";
            case LT:
                return "lt";
            case GT:
                return "gt";
            case LEQ:
                return "leq";
            case GEQ:
                return "geq";
            case EQ:
                return "eq";
            case NEQ:
                return "neq";
            case CONDITIONAL_AND:
                return "and";
            case CONDITIONAL_OR:
                return "or";
            case ASSIGN:
                return "assign";
            case NOT:
                return "not";
            default:
                throw new IllegalArgumentException("operator " + operator + " not recognized");
        }
    }

    public static String getUnaryOperatorName(String operator) {
        switch (operator) {
            case MINUS:
                return "neg";
            case NOT:
                return "not";
            default:
                throw new IllegalArgumentException("unary operator " + operator + " not recognized");
        }
    }

    public static String getColoredOperatorName(String operator) {
        return Utils.coloredPrint(getOperatorName(operator), Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
    }

    public static String getColoredUnaryOperatorName(String operator) {
        return Utils.coloredPrint(getUnaryOperatorName(operator), Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
    }
}
