package decaf.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.ezylang.evalex.config.ExpressionConfiguration;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.FormalArgument;

public class Utils {
  public static final String SPACE = " ";
  public static String DEFAULT_INDENT = "    ";

  public static String getStringFromInputStream(
      InputStream inputStream,
      Logger logger
  ) {
    String str;
    try {
      str = new String(
          inputStream.readAllBytes(),
          UTF_8
      );
    } catch (IOException e) {
      logger.log(
          Level.SEVERE,
          "Cannot read input stream"
      );
      str = "";
    }
    return str;
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<T> findAllOfType(
      AST root,
      Class<T> tClass
  ) {
    Set<T> results = new HashSet<>();
    Stack<AST> toExplore = new Stack<>();
    toExplore.add(root);
    while (!toExplore.isEmpty()) {
      final AST node = toExplore.pop();
      if (tClass.isAssignableFrom(node.getClass())) {
        results.add((T) node);
      } else {
        for (Pair<String, AST> astPair : node.getChildren()) {
          toExplore.add(astPair.second());
        }
      }
    }
    return results;
  }

  public static String coloredPrint(
      String string,
      String color
  ) {
    return color + string + ANSIColorConstants.ANSI_RESET;
  }

  public static String indentBlock(Block body) {
    String blockString = body.getSourceCode();
    return indentBlock(blockString);
  }

  public static String indentBlock(String blockString) {
    return indentBlock(
        blockString,
        1
    );
  }

  public static String indentBlock(
      String blockString,
      int count
  ) {
    var indent = "    ".repeat(count);
    List<String> list = new ArrayList<>();
    for (String s : blockString.split("\n")) {
      String s1 = indent + s;
      list.add(s1);
    }
    return String.join(
        "\n",
        list
    );
  }

  public static String identBlockWithNumbering(
      String blockString,
      int startingLineNumber
  ) {
    return identBlockWithNumbering(
        blockString.split("\n"),
        DEFAULT_INDENT,
        startingLineNumber
    );
  }

  public static String identAndNumberOneLine(
      @NotNull String line,
      @NotNull String indent,
      int lineNumber,
      int numDigits
  ) {
    return String.format(
        "%s%s | %s",
        indent,
        Utils.coloredPrint(
            String.format(
                "%" + numDigits + "d",
                lineNumber
            ),
            ANSIColorConstants.ANSI_BLUE
        ),
        line
    );
  }

  public static String identPointNumberOneLine(
      @NotNull String line,
      @NotNull String subIndent,
      int lineNumber,
      int numDigits
  ) {
    return String.format(
        "%s %s  %s | %s",
        subIndent,
        Utils.coloredPrint(
            ">",
            Utils.ANSIColorConstants.ANSI_GREEN_BOLD
        ),
        Utils.coloredPrint(
            String.format(
                "%" + numDigits + "d",
                lineNumber
            ),
            Utils.ANSIColorConstants.ANSI_BLUE
        ),
        line
    );
  }

  public static String identBlockWithNumbering(
      String[] lines,
      String indent,
      int startingLineNumber
  ) {
    return identBlockWithNumbering(
        lines,
        indent,
        startingLineNumber,
        (int) (Math.log10(lines.length) + 1)
    );
  }

  public static String identBlockWithNumbering(
      String[] lines,
      String indent,
      int startingLineNumber,
      int numDigits
  ) {
    assert numDigits >= (int) (Math.log10(lines.length) + 1);

    return IntStream.range(
                        startingLineNumber,
                        startingLineNumber + lines.length
                    )
                    .mapToObj(
                        lineNumber -> identAndNumberOneLine(
                            lines[lineNumber - startingLineNumber],
                            indent,
                            lineNumber,
                            numDigits
                        )
                    )
                    .collect(Collectors.joining("\n"));
  }

  public static boolean containsAlphabeticCharacters(String string) {
    return string.matches(".*[a-zA-Z]+.*");
  }

  public static long symbolicallyEvaluateUnaryInstruction(
      String operator,
      long value
  ) {
    if (operator.equals(Operators.NOT)) {
      if (value == 1L) {
        return 0L;
      } else {
        return 1L;
      }
    } else if (operator.equals(Operators.MINUS)) return -value;
    throw new IllegalArgumentException("unsupported unary operator " + operator);
  }

  public static Optional<Long> symbolicallyEvaluate(String string) {
    // this check is necessary because the evaluator evaluates variables like 'e' and 'pi'
    if (containsAlphabeticCharacters(string)) {
      return Optional.empty();
    }
    var expressionConfiguration =
        ExpressionConfiguration.builder()
                               .mathContext(new MathContext(
                                   100,
                                   RoundingMode.HALF_UP
                               ))
                               .decimalPlacesRounding(2)
                               .build();
    var expression = new com.ezylang.evalex.Expression(
        string,
        expressionConfiguration
    );
    try {
      var res = expression.evaluate();
      return Optional.of(res.getNumberValue()
                            .longValue());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static String escapeMetaCharacters(String inputString) {
    return inputString.replaceAll(
        "\n",
        "NEWLINE"
    );
  }

  public static int roundUp16(int n) {
    if (n == 0) return 16;
    return n >= 0 ? ((n + 16 - 1) / 16) * 16: (n / 16) * 16;
  }

  public interface ANSIColorConstants {

    String ANSI_RESET = "\u001B[0m";

    String ANSI_BLACK = "\u001B[30m";
    String ANSI_RED = "\u001B[31m";
    String ANSI_GREEN = "\u001B[32m";
    String ANSI_YELLOW = "\u001B[33m";
    String ANSI_BLUE = "\u001B[34m";
    String ANSI_PURPLE = "\u001B[35m";
    String ANSI_CYAN = "\u001B[36m";
    String ANSI_WHITE = "\u001B[37m";
    String LIGHT_GREY = "\u001B[38;5;250m";

    String ANSI_PURPLE_BOLD = "\u001B[35;1m";
    String ANSI_BLUE_BOLD = "\u001B[34;1m";
    String ANSI_GREEN_BOLD = "\u001B[32;1m";
    String ANSI_RED_BOLD = "\u001B[31;1m";
    String ANSI_BLACK_BOLD = "\u001B[30;1m";

    String ANSI_BRIGHT_RED = "\u001B[91m";
    String ANSI_BRIGHT_BLACK = "\u001B[90m";
    String ANSI_BRIGHT_GREEN = "\u001B[92m";
    String ANSI_BRIGHT_YELLOW = "\u001B[93m";
    String ANSI_BRIGHT_BLUE = "\u001B[94m";
    String ANSI_BRIGHT_PURPLE = "\u001B[95m";
    String ANSI_BRIGHT_CYAN = "\u001B[96m";
    String ANSI_BRIGHT_WHITE = "\u001B[97m";

    String[] FOREGROUNDS = {ANSI_BLACK, ANSI_RED, ANSI_GREEN, ANSI_YELLOW, ANSI_BLUE, ANSI_PURPLE, ANSI_CYAN, ANSI_WHITE, ANSI_BRIGHT_BLACK, ANSI_BRIGHT_RED, ANSI_BRIGHT_GREEN, ANSI_BRIGHT_YELLOW, ANSI_BRIGHT_BLUE, ANSI_BRIGHT_PURPLE, ANSI_BRIGHT_CYAN, ANSI_BRIGHT_WHITE};

    String ANSI_BG_BLACK = "\u001B[40m";
    String ANSI_BG_RED = "\u001B[41m";
    String ANSI_BG_GREEN = "\u001B[42m";
    String ANSI_BG_YELLOW = "\u001B[43m";
    String ANSI_BG_BLUE = "\u001B[44m";
    String ANSI_BG_PURPLE = "\u001B[45m";
    String ANSI_BG_CYAN = "\u001B[46m";
    String ANSI_BG_WHITE = "\u001B[47m";

    String ANSI_BRIGHT_BG_BLACK = "\u001B[100m";
    String ANSI_BRIGHT_BG_RED = "\u001B[101m";
    String ANSI_BRIGHT_BG_GREEN = "\u001B[102m";
    String ANSI_BRIGHT_BG_YELLOW = "\u001B[103m";
    String ANSI_BRIGHT_BG_BLUE = "\u001B[104m";
    String ANSI_BRIGHT_BG_PURPLE = "\u001B[105m";
    String ANSI_BRIGHT_BG_CYAN = "\u001B[106m";
    String ANSI_BRIGHT_BG_WHITE = "\u001B[107m";

    String[] BACKGROUNDS = {ANSI_BG_BLACK, ANSI_BG_RED, ANSI_BG_GREEN, ANSI_BG_YELLOW, ANSI_BG_BLUE, ANSI_BG_PURPLE, ANSI_BG_CYAN, ANSI_BG_WHITE, ANSI_BRIGHT_BG_BLACK, ANSI_BRIGHT_BG_RED, ANSI_BRIGHT_BG_GREEN, ANSI_BRIGHT_BG_YELLOW, ANSI_BRIGHT_BG_BLUE, ANSI_BRIGHT_BG_PURPLE, ANSI_BRIGHT_BG_CYAN, ANSI_BRIGHT_BG_WHITE};
  }

  public static String
  prettyPrintMethodFormalArguments(@NotNull List<FormalArgument> formalArgumentList) {
    if (formalArgumentList.isEmpty()) {
      return "";
    }
    StringBuilder stringBuilder = new StringBuilder();
    for (FormalArgument formalArgument :
        formalArgumentList) {
      stringBuilder.append(
          Utils.coloredPrint(
              formalArgument.getType()
                            .toString(),
              Utils.ANSIColorConstants.ANSI_PURPLE
          ));
      stringBuilder.append(" ");
      stringBuilder.append(
          Utils.coloredPrint(
              formalArgument.getName(),
              Utils.ANSIColorConstants.ANSI_WHITE
          ));
      stringBuilder.append(", ");
    }
    stringBuilder.delete(
        stringBuilder.length() - 2,
        stringBuilder.length()
    );
    return stringBuilder.toString();
  }

  private static void addTerminal(
      @NotNull Pair<String, AST> labelAndNode, String prefix,
      @NotNull String connector,
      @NotNull List<String> tree
  ) {
    if (!labelAndNode.second()
                     .isTerminal())
      throw new IllegalArgumentException();
    tree.add(prefix + connector + " " +
                 Utils.coloredPrint(
                     labelAndNode.first(),
                     Utils.ANSIColorConstants.ANSI_BLUE
                 ) +
                 " = " + labelAndNode.second());
  }

  private static void addNonTerminal(
      @NotNull Pair<String, AST> labelAndNode, int index,
      int numChildren, @NotNull String prefix,
      @NotNull String connector, @NotNull List<String> tree
  ) {
    tree.add(String.format(
        "%s%s %s = [%s]",
        prefix,
        connector,
        labelAndNode.first(),
        Utils.coloredPrint(
            labelAndNode.second()
                        .getSourceCode(),
            Utils.ANSIColorConstants.ANSI_PURPLE
        )
    ));
    prefix += (index != numChildren - 1) ? PrintConstants.PIPE_PREFIX
        : PrintConstants.SPACE_PREFIX;
    treeBody(
        labelAndNode,
        tree,
        prefix
    );
  }

  private static void treeBody(
      @NotNull Pair<String, AST> parentNode, List<String> tree,
      String prefix
  ) {
    List<Pair<String, AST>> nodeList = parentNode.second()
                                                 .getChildren();
    for (int i = 0; i < nodeList.size(); i++) {
      final String connector = (i == nodeList.size() - 1) ? PrintConstants.ELBOW
          : PrintConstants.TEE;
      final Pair<String, AST> labelAndNode = nodeList.get(i);
      if (labelAndNode.second()
                      .isTerminal())
        addTerminal(
            labelAndNode,
            prefix,
            connector,
            tree
        );
      else
        addNonTerminal(
            labelAndNode,
            i,
            nodeList.size(),
            prefix,
            connector,
            tree
        );
    }
  }

  public static void printParseTree(@NotNull AST root) {
    var tree = new ArrayList<String>();
    treeBody(
        new Pair<>(
            ".",
            root
        ),
        tree,
        ""
    );
    while (!tree.isEmpty() && tree.get(tree.size() - 1)
               .isEmpty())
      tree.remove(tree.size() - 1);
    for (String s : tree) {
      System.out.println(s);
    }
  }

  private static class PrintConstants {
    public static final String ELBOW = "└──";
    public static final String TEE = "├──";
    public static final String PIPE_PREFIX = "│   ";
    public static final String SPACE_PREFIX = "    ";
  }
}
