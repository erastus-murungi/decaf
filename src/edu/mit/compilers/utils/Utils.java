package edu.mit.compilers.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
  public static final String SPACE = " ";
  public static final String EMPTY = "";

  // Adapted from
  // https://github.com/apache/commons-lang/blob/3adea508fbf48deb760eb71c7239dbd0ba351830/src/main/java/org/apache/commons/lang3/StringUtils.java#L5570
  public static String canonicalizeWhiteSpace(final String str) {
    if (str.isEmpty()) {
      return str;
    }
    final int size = str.length();
    final char[] newChars = new char[size];
    int count = 0;
    int whitespacesCount = 0;
    boolean startWhitespaces = true;

    for (int i = 0; i < size; i++) {
      final char actualChar = str.charAt(i);
      final boolean isWhitespace = Character.isWhitespace(actualChar);
      if (isWhitespace) {
        if (whitespacesCount == 0 && !startWhitespaces) {
          newChars[count++] = SPACE.charAt(0);
        }
        whitespacesCount++;
      } else {
        startWhitespaces = false;
        newChars[count++] = (actualChar == 160 ? 32 : actualChar);
        whitespacesCount = 0;
      }
    }
    if (startWhitespaces) {
      return EMPTY;
    }
    return new String(newChars, 0, count - (whitespacesCount > 0 ? 1 : 0)).trim();
  }

  private static String replace(final Pattern pattern, final String original) {
    int lastIndex = 0;
    StringBuilder output = new StringBuilder();
    Matcher matcher = pattern.matcher(original);
    while (matcher.find()) {
      final String s = matcher.group(0);
      output.append(original, lastIndex, matcher.start()).append(s.startsWith("/") ? SPACE : s);
      lastIndex = matcher.end();
    }
    if (lastIndex < original.length()) {
      output.append(original, lastIndex, original.length());
    }
    return output.toString();
  }

  // Adapted from:
  // https://stackoverflow.com/questions/241327/remove-c-and-c-comments-using-python
  public static String stripAllComments(String str) {
    final Pattern pattern = Pattern.compile(
        "//.*?$|/\\*.*?\\*/|'(?:\\\\.|[^\\\\'])*'|\"(?:\\\\.|[^\\\\\"])*\"",
        Pattern.DOTALL | Pattern.MULTILINE);
    final String ret = replace(pattern, str);
    if (ret.contains("*")) {
      throw new IllegalArgumentException("Nested block comments found");
    }
    return ret;
  }

  public static String coloredPrint(String string, String color) {
    return color + string + ANSIColorConstants.ANSI_RESET;
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

    String ANSI_BRIGHT_RED = "\u001B[91m";
    String ANSI_BRIGHT_BLACK = "\u001B[90m";
    String ANSI_BRIGHT_GREEN = "\u001B[92m";
    String ANSI_BRIGHT_YELLOW = "\u001B[93m";
    String ANSI_BRIGHT_BLUE = "\u001B[94m";
    String ANSI_BRIGHT_PURPLE = "\u001B[95m";
    String ANSI_BRIGHT_CYAN = "\u001B[96m";
    String ANSI_BRIGHT_WHITE = "\u001B[97m";

    String[] FOREGROUNDS = {
        ANSI_BLACK, ANSI_RED, ANSI_GREEN, ANSI_YELLOW,
        ANSI_BLUE, ANSI_PURPLE, ANSI_CYAN, ANSI_WHITE,
        ANSI_BRIGHT_BLACK, ANSI_BRIGHT_RED, ANSI_BRIGHT_GREEN, ANSI_BRIGHT_YELLOW,
        ANSI_BRIGHT_BLUE, ANSI_BRIGHT_PURPLE, ANSI_BRIGHT_CYAN, ANSI_BRIGHT_WHITE
    };

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

    String[] BACKGROUNDS = {
        ANSI_BG_BLACK, ANSI_BG_RED, ANSI_BG_GREEN, ANSI_BG_YELLOW,
        ANSI_BG_BLUE, ANSI_BG_PURPLE, ANSI_BG_CYAN, ANSI_BG_WHITE,
        ANSI_BRIGHT_BG_BLACK, ANSI_BRIGHT_BG_RED, ANSI_BRIGHT_BG_GREEN, ANSI_BRIGHT_BG_YELLOW,
        ANSI_BRIGHT_BG_BLUE, ANSI_BRIGHT_BG_PURPLE, ANSI_BRIGHT_BG_CYAN, ANSI_BRIGHT_BG_WHITE
    };
  }
}