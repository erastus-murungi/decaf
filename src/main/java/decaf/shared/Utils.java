package decaf.shared;

import com.ezylang.evalex.config.ExpressionConfiguration;
import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.FormalArgument;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Utils {
    public static final String SPACE = " ";
    public static String DEFAULT_INDENT = "    ";

    public static String getStringFromInputStream(InputStream inputStream, Logger logger) {
        String str;
        try {
            str = new String(inputStream.readAllBytes(), UTF_8);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot read input stream");
            str = "";
        }
        return str;
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> findAllOfType(AST root, Class<T> tClass) {
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

    public static String indentBlock(Block body) {
        String blockString = body.getSourceCode();
        return indentBlock(blockString);
    }

    public static String indentBlock(String blockString) {
        return indentBlock(blockString, 1);
    }

    public static String indentBlock(String blockString, int count) {
        var indent = "    ".repeat(count);
        List<String> list = new ArrayList<>();
        for (String s : blockString.split("\n")) {
            String s1 = indent + s;
            list.add(s1);
        }
        return String.join("\n", list);
    }

    public static String identBlockWithNumbering(String blockString, int startingLineNumber) {
        return identBlockWithNumbering(blockString.split("\n"), DEFAULT_INDENT, startingLineNumber);
    }

    public static String identAndNumberOneLine(@NotNull String line,
                                               @NotNull String indent,
                                               int lineNumber,
                                               int numDigits) {
        return String.format("%s%s | %s",
                             indent,
                             ColorPrint.getColoredString(String.format("%" + numDigits + "d", lineNumber),
                                                         ColorPrint.Color.BLUE
                                                        ),
                             line
                            );
    }

    public static String identPointNumberOneLine(@NotNull String line,
                                                 @NotNull String subIndent,
                                                 int lineNumber,
                                                 int numDigits) {
        return String.format("%s %s  %s | %s",
                             subIndent,
                             ColorPrint.getColoredString(">", ColorPrint.Color.GREEN, ColorPrint.Format.BOLD),
                             ColorPrint.getColoredString(String.format("%" + numDigits + "d", lineNumber),
                                                         ColorPrint.Color.BLUE
                                                        ),
                             line
                            );
    }

    public static String identBlockWithNumbering(String[] lines, String indent, int startingLineNumber) {
        return identBlockWithNumbering(lines, indent, startingLineNumber, (int) (Math.log10(lines.length) + 1));
    }

    public static String identBlockWithNumbering(String[] lines, String indent, int startingLineNumber, int numDigits) {
        assert numDigits >= (int) (Math.log10(lines.length) + 1);

        return IntStream.range(startingLineNumber, startingLineNumber + lines.length)
                        .mapToObj(lineNumber -> identAndNumberOneLine(lines[lineNumber - startingLineNumber],
                                                                      indent,
                                                                      lineNumber,
                                                                      numDigits
                                                                     ))
                        .collect(Collectors.joining("\n"));
    }

    public static boolean containsAlphabeticCharacters(String string) {
        return string.matches(".*[a-zA-Z]+.*");
    }

    public static long symbolicallyEvaluateUnaryInstruction(String operator, long value) {
        if (operator.equals(Operators.NOT)) {
            if (value == 1L) {
                return 0L;
            } else {
                return 1L;
            }
        } else if (operator.equals(Operators.MINUS)) {
          return -value;
        }
        throw new IllegalArgumentException("unsupported unary operator " + operator);
    }

    public static Optional<Long> symbolicallyEvaluate(String string) {
        // this check is necessary because the evaluator evaluates variables like 'e' and 'pi'
        if (containsAlphabeticCharacters(string)) {
            return Optional.empty();
        }
        var expressionConfiguration = ExpressionConfiguration.builder()
                                                             .mathContext(new MathContext(100, RoundingMode.HALF_UP))
                                                             .decimalPlacesRounding(2)
                                                             .build();
        var expression = new com.ezylang.evalex.Expression(string, expressionConfiguration);
        try {
            var res = expression.evaluate();
            return Optional.of(res.getNumberValue().longValue());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String escapeMetaCharacters(String inputString) {
        return inputString.replaceAll("\n", "NEWLINE");
    }

    public static int roundUp16(int n) {
      if (n == 0) {
        return 16;
      }
        return n >= 0 ? ((n + 16 - 1) / 16) * 16 : (n / 16) * 16;
    }

    public static String prettyPrintMethodFormalArguments(@NotNull List<FormalArgument> formalArgumentList) {
        if (formalArgumentList.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (FormalArgument formalArgument : formalArgumentList) {
            stringBuilder.append(ColorPrint.getColoredString(formalArgument.getType().toString(),
                                                             ColorPrint.Color.MAGENTA,
                                                             ColorPrint.Format.BOLD
                                                            ));
            stringBuilder.append(" ");
            stringBuilder.append(ColorPrint.getColoredString(formalArgument.getName(), ColorPrint.Color.WHITE));
            stringBuilder.append(", ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        return stringBuilder.toString();
    }

    private static void addTerminal(@NotNull Pair<String, AST> labelAndNode,
                                    String prefix,
                                    @NotNull String connector,
                                    @NotNull List<String> tree) {
      if (!labelAndNode.second().isTerminal()) {
        throw new IllegalArgumentException();
      }
        tree.add(prefix +
                 connector +
                 " " +
                 ColorPrint.getColoredString(labelAndNode.first(), ColorPrint.Color.BLUE) +
                 " = " +
                 labelAndNode.second());
    }

    private static void addNonTerminal(@NotNull Pair<String, AST> labelAndNode,
                                       int index,
                                       int numChildren,
                                       @NotNull String prefix,
                                       @NotNull String connector,
                                       @NotNull List<String> tree) {
        tree.add(String.format("%s%s %s = [%s]",
                               prefix,
                               connector,
                               labelAndNode.first(),
                               ColorPrint.getColoredString(labelAndNode.second().getSourceCode(),
                                                           ColorPrint.Color.MAGENTA,
                                                           ColorPrint.Format.BOLD
                                                          )
                              ));
        prefix += (index != numChildren - 1) ? PrintConstants.PIPE_PREFIX : PrintConstants.SPACE_PREFIX;
        treeBody(labelAndNode, tree, prefix);
    }

    private static void treeBody(@NotNull Pair<String, AST> parentNode, List<String> tree, String prefix) {
        List<Pair<String, AST>> nodeList = parentNode.second().getChildren();
        for (int i = 0; i < nodeList.size(); i++) {
            final String connector = (i == nodeList.size() - 1) ? PrintConstants.ELBOW : PrintConstants.TEE;
            final Pair<String, AST> labelAndNode = nodeList.get(i);
          if (labelAndNode.second().isTerminal()) {
            addTerminal(labelAndNode, prefix, connector, tree);
          } else {
            addNonTerminal(labelAndNode, i, nodeList.size(), prefix, connector, tree);
          }
        }
    }

    public static void printParseTree(@NotNull AST root) {
        var tree = new ArrayList<String>();
        treeBody(new Pair<>(".", root), tree, "");
      while (!tree.isEmpty() && tree.get(tree.size() - 1).isEmpty()) {
        tree.remove(tree.size() - 1);
      }
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
