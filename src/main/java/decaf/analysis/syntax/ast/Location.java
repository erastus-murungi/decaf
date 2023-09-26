package decaf.analysis.syntax.ast;


import org.jetbrains.annotations.NotNull;

public abstract class Location extends Expression {
  @NotNull protected final RValue rValue;

  public Location(@NotNull RValue rValue) {
    super(rValue.getTokenPosition());
    this.rValue = rValue;
  }

  public String getLabel() {
    return rValue.getLabel();
  }
}
