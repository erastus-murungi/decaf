package decaf.analysis.syntax.ast;


public abstract class Location extends Expression {
  public final RValue RValue;

  public Location(RValue RValue) {
    super(RValue.tokenPosition);
    this.RValue = RValue;
  }
}
