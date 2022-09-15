package decaf.dataflow.ssapasses;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import decaf.codegen.codes.Instruction;
import decaf.codegen.names.IrValue;
import decaf.common.Utils;

public record SSACopyOptResult(Instruction before, @Nullable Instruction after, @Nullable IrValue replaced,
                               IrValue replacer) {
    public static String printResults(Collection<SSACopyOptResult> results) {
        var output = new ArrayList<String>();
        if (!results.isEmpty()) {
            int longestMessage = results.stream()
                    .map(ssaCopyOptResult -> ssaCopyOptResult.before.noCommentsToString()
                            .strip()
                            .length())
                    .max(Comparator.comparingInt(a -> a))
                    .orElseThrow();
            output.add(String.format("Optimization name :: %s", Utils.coloredPrint("Sparse Conditional Constant Propagation (SCCP)", Utils.ANSIColorConstants.ANSI_GREEN_BOLD)));
            output.add(
                    String.format("%s | %s | %s", "Before" + " ".repeat(longestMessage - 6), "After" + " ".repeat(longestMessage - 5), "Okay")
            );
            for (SSACopyOptResult ssaCopyOptResult : results) {
                if (ssaCopyOptResult.after != null)
                    output.add(String.format("%s | %s | %s", ssaCopyOptResult.before.noCommentsSyntaxHighlighted(), ssaCopyOptResult.after.noCommentsSyntaxHighlighted(), "replaced instruction"));
            }
        }
        return String.join("\n", output);
    }
}
