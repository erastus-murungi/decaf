package edu.mit.compilers.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
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

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.AllocateInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;

public class Utils {
    // adopted from Java 15
    public static final long WORD_SIZE = 8;

    public static final String SPACE = " ";
    public static final String EMPTY = "";

    public static String getStringFromInputStream(InputStream inputStream) {
        String str;
        try {
            str = new String(inputStream.readAllBytes(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
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

    private static String replace(final Pattern pattern, final String original) {
        int lastIndex = 0;
        StringBuilder output = new StringBuilder();
        Matcher matcher = pattern.matcher(original);
        while (matcher.find()) {
            final String s = matcher.group(0);
            output
                    .append(original, lastIndex, matcher.start())
                    .append(s.startsWith("/") ? SPACE : s);
            lastIndex = matcher.end();
        }
        if (lastIndex < original.length()) {
            output.append(original, lastIndex, original.length());
        }
        return output.toString();
    }

    public static String coloredPrint(String string, String color) {
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
        return String.join("\n", list);
    }

    public static void insertAllocateInstructions(ProgramIr programIr) {
        programIr.methodList.forEach(
                method -> method.entryBlock.getInstructionList()
                        .addAll(1, ProgramIr.getLocals(method, programIr.globals)
                                .stream()
                                .map(AllocateInstruction::new)
                                .toList()
                        )
        );

    }

    public static boolean containsAlphabeticCharacters(String string) {
        return string.matches(".*[a-zA-Z]+.*");
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

    public static void printSsaCfg(Collection<Method> methodCollection, String filename) {
        var copy = new HashMap<String, BasicBlock>();
        methodCollection.forEach(methodBegin -> copy.put(methodBegin.methodName(), methodBegin.entryBlock));
        GraphVizPrinter.printGraph(copy,
                (basicBlock -> basicBlock.getInstructionList()
                        .stream()
                        .map(Instruction::toString)
                        .collect(Collectors.joining("\n"))),
                filename
        );
    }

    public static Set<LValue> getAllLValuesInInstructionList(InstructionList instructionList) {
        return instructionList.stream()
                .flatMap(instruction -> instruction.getAllLValues()
                        .stream())
                .collect(Collectors.toSet());

    }

    public static Set<LValue> getAllLValuesInBasicBlocks(List<BasicBlock> basicBlocks) {
        return (
                basicBlocks.stream()
                        .flatMap(basicBlock -> basicBlock.getInstructionList()
                                .stream())
                        .flatMap(instruction -> instruction.getAllValues()
                                .stream())
                        .filter(abstractName -> abstractName instanceof LValue)
                        .map(abstractName -> (LValue) abstractName)
                        .collect(Collectors.toUnmodifiableSet())
        );
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
