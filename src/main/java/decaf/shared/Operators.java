package decaf.shared;


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
    return switch (operator) {
      case PLUS -> "add";
      case MINUS -> "sub";
      case DIVIDE -> "idiv";
      case MULTIPLY -> "mul";
      case MOD -> "mod";
      case LT -> "lt";
      case GT -> "gt";
      case LEQ -> "leq";
      case GEQ -> "geq";
      case EQ -> "eq";
      case NEQ -> "neq";
      case CONDITIONAL_AND -> "and";
      case CONDITIONAL_OR -> "or";
      case ASSIGN -> "assign";
      case NOT -> "not";
      default -> throw new IllegalArgumentException("operator " + operator + " not recognized");
    };
  }

  public static String getUnaryOperatorName(String operator) {
    return switch (operator) {
      case MINUS -> "neg";
      case NOT -> "not";
      default -> throw new IllegalArgumentException("unary operator " + operator + " not recognized");
    };
  }

  public static String getColoredOperatorName(String operator) {
    return ColorPrint.getColoredString(
            getOperatorName(operator),
            ColorPrint.Color.GREEN, ColorPrint.Format.BOLD
    );
  }

  public static String getColoredUnaryOperatorName(String operator) {
    return ColorPrint.getColoredString(
            getUnaryOperatorName(operator),
            ColorPrint.Color.GREEN, ColorPrint.Format.BOLD
    );
  }

  public static boolean isCompoundOperator(String operator) {
    return switch (operator) {
      case ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> true;
      default -> false;
    };
  }
}
