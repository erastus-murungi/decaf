package decaf.analysis.syntax.ast;


import org.jetbrains.annotations.NotNull;

public abstract class Location extends Expression {
  @NotNull protected final RValue rValue;

  public Location(@NotNull RValue rValue) {
    super(rValue.tokenPosition);
    this.rValue = rValue;
  }

  public String getLabel() {
    return rValue.getLabel();
  }
}
