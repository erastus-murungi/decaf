package decaf.synthesis.asm.instructions;


public class X86MetaData extends X64Instruction {
  private final String metaData;

  public X86MetaData(String metaData) {
    this.metaData = metaData;
    verifyConstruction();
  }

  public static X86MetaData blockComment(String comment) {
    return new X86MetaData(String.format(
        "/* %s */",
        comment
    ));
  }

  @Override
  protected void verifyConstruction() {
  }

  @Override
  public String toString() {
    return metaData;
  }
}
