package decaf.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import decaf.ast.AST;
import decaf.ast.BinaryOpExpression;
import decaf.ast.Block;
import decaf.ast.Expression;
import decaf.ast.ExpressionParameter;
import decaf.ast.LocationArray;
import decaf.ast.MethodCall;
import decaf.ast.MethodCallParameter;
import decaf.ast.ParenthesizedExpression;
import decaf.ast.UnaryOpExpression;
import decaf.cfg.BasicBlock;
import decaf.codegen.InstructionList;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.names.IrAssignableValue;
import decaf.codegen.names.IrRegister;

public class Utils {
  // adopted from Java 15
  public static final int WORD_SIZE = 8;

  public static final String SPACE = " ";

  public static String getStringFromInputStream(InputStream inputStream) {
    String str;
    try {
      str = new String(
          inputStream.readAllBytes(),
          UTF_8
      );
    } catch (IOException e) {
      e.printStackTrace();
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

  private static String replace(
      final Pattern pattern,
      final String original
  ) {
    int lastIndex = 0;
    StringBuilder output = new StringBuilder();
    Matcher matcher = pattern.matcher(original);
    while (matcher.find()) {
      final String s = matcher.group(0);
      output.append(
                original,
                lastIndex,
                matcher.start()
            )
            .append(s.startsWith("/") ? SPACE: s);
      lastIndex = matcher.end();
    }
    if (lastIndex < original.length()) {
      output.append(
          original,
          lastIndex,
          original.length()
      );
    }
    return output.toString();
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
    List<String> list = new ArrayList<>();
    for (String s : blockString.split("\n")) {
      String s1 = "    " + s;
      list.add(s1);
    }
    return String.join(
        "\n",
        list
    );
  }

  public static boolean containsAlphabeticCharacters(String string) {
    return string.matches(".*[a-zA-Z]+.*");
  }

  public static long symbolicallyEvaluateUnaryInstruction(
      @NotNull String operator,
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
    var expression = new com.udojava.evalex.Expression(string);
    try {
      var res = expression.setPrecision(100)
                          .eval();
      return Optional.of(res.longValue());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static void printSsaCfg(
      Collection<Method> methodCollection,
      String filename
  ) {
    var copy = new HashMap<String, BasicBlock>();
    methodCollection.forEach(methodBegin -> copy.put(
        methodBegin.methodName(),
        methodBegin.getEntryBlock()
    ));
    GraphVizManager.printGraph(
        copy,
        (basicBlock -> basicBlock.getInstructionList()
                                 .stream()
                                 .map(Instruction::toString)
                                 .collect(Collectors.joining("\n"))),
        filename
    );
  }

  public static Set<IrAssignableValue> getAllLValuesInInstructionList(InstructionList instructionList) {
    return instructionList.stream()
                          .flatMap(instruction -> instruction.getAllLValues()
                                                             .stream())
                          .collect(Collectors.toSet());

  }

  public static Set<IrAssignableValue> genRegAllocatableValuesFromInstructions(Collection<Instruction> instructionList) {
    return instructionList.stream()
                          .flatMap(instruction -> instruction.getAllRegisterAllocatableValues()
                                                             .stream())
                          .collect(Collectors.toSet());

  }

  public static Set<IrAssignableValue> getAllLValuesInBasicBlocks(List<BasicBlock> basicBlocks) {
    return (basicBlocks.stream()
                       .flatMap(basicBlock -> basicBlock.getInstructionList()
                                                        .stream())
                       .flatMap(instruction -> instruction.getAllLValues()
                                                          .stream())
                       .collect(Collectors.toUnmodifiableSet()));
  }

  public static Set<IrRegister> getAllVirtualRegistersInBasicBlocks(List<BasicBlock> basicBlocks) {
    return (basicBlocks.stream()
                       .flatMap(basicBlock -> basicBlock.getInstructionList()
                                                        .stream())
                       .flatMap(instruction -> instruction.getAllVirtualRegisters()
                                                          .stream())
                       .collect(Collectors.toUnmodifiableSet()));
  }

  public static Expression rotateBinaryOpExpression(Expression expr) {
    if (expr instanceof BinaryOpExpression) {
      if (((BinaryOpExpression) expr).rhs instanceof BinaryOpExpression rhsTemp) {
        if (BinaryOpExpression.operatorPrecedence.get(((BinaryOpExpression) expr).op.getSourceCode())
                                                 .equals(BinaryOpExpression.operatorPrecedence.get(rhsTemp.op.getSourceCode()))) {
          ((BinaryOpExpression) expr).rhs = rhsTemp.lhs;
          rhsTemp.lhs = expr;
          ((BinaryOpExpression) expr).lhs = rotateBinaryOpExpression(((BinaryOpExpression) expr).lhs);
          ((BinaryOpExpression) expr).rhs = rotateBinaryOpExpression(((BinaryOpExpression) expr).rhs);
          return rotateBinaryOpExpression(rhsTemp);
        }
      }
      ((BinaryOpExpression) expr).lhs = rotateBinaryOpExpression(((BinaryOpExpression) expr).lhs);
      ((BinaryOpExpression) expr).rhs = rotateBinaryOpExpression(((BinaryOpExpression) expr).rhs);
    } else if (expr instanceof ParenthesizedExpression) {
      rotateBinaryOpExpression(((ParenthesizedExpression) expr).expression);
    } else if (expr instanceof MethodCall) {
      for (int i = 0; i < ((MethodCall) expr).methodCallParameterList.size(); i++) {
        MethodCallParameter param = ((MethodCall) expr).methodCallParameterList.get(i);
        if (param instanceof ExpressionParameter) {
          ((MethodCall) expr).methodCallParameterList.set(
              i,
              new ExpressionParameter(rotateBinaryOpExpression(((ExpressionParameter) param).expression))
          );
        }
      }
    } else if (expr instanceof LocationArray) {
      rotateBinaryOpExpression(((LocationArray) expr).expression);
    } else if (expr instanceof UnaryOpExpression) {
      rotateBinaryOpExpression(((UnaryOpExpression) expr).operand);
    }
    return expr;
  }

  public static int roundUp16(int n) {
    if (n == 0) return 16;
    return n >= 0 ? ((n + 16 - 1) / 16) * 16: (n / 16) * 16;
  }

  public static boolean isReachable(
      @NotNull BasicBlock source,
      @NotNull BasicBlock destination
  ) {
    var workList = new ArrayDeque<BasicBlock>();
    workList.offer(source);
    var explored = new HashSet<BasicBlock>();
    while (!workList.isEmpty()) {
      var current = workList.remove();
      if (explored.contains(current)) continue;
      explored.add(current);
      if (destination == current) return true;
      for (var successor : current.getSuccessors()) {
        workList.offer(successor);
      }
    }
    return false;
  }

  private String removeParentheses(String s) {
    s = s.strip();
    if (s.startsWith("(")) {
      return removeParentheses(s.strip()
                                .substring(
                                    1,
                                    s.length() - 1
                                ));
    }
    return s;
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

    String ANSI_PURPLE_BOLD = "\u001B[35;1m";
    String ANSI_BLUE_BOLD = "\u001B[34;1m";
    String ANSI_GREEN_BOLD = "\u001B[32;1m";

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
}
