package decaf.ir.dataflow.ssapasses;

import decaf.ir.codes.Instruction;

public record SCCPOptResult(Instruction before, Instruction after) {

  @Override
  public String toString() {
    return String.format(
        "SCCP :: replaced %s with %s",
        before.noCommentsSyntaxHighlighted()
              .strip(),
        after.noCommentsSyntaxHighlighted()
             .strip()
    );
  }
}
