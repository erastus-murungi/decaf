package decaf.synthesis.asm.instructions;


import java.util.Objects;

public abstract class X64Instruction {
  private String comment;

  public X64Instruction(String comment) {
    this.comment = comment;
  }

  public X64Instruction() {
    this.comment = "";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    X64Instruction x64Instruction = (X64Instruction) o;
    return Objects.equals(this.toString()
                              .strip(),
                          x64Instruction.toString()
                                        .strip()
    );
  }

  public String noCommentToString() {
    var commentCopy = comment;
    comment = "";
    var toString = toString();
    comment = commentCopy;
    return toString;
  }

  protected abstract void verifyConstruction();
}
