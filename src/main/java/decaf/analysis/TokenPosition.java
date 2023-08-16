package decaf.analysis;


public record TokenPosition(int line, int column, int offset) {

  @Override
  public String toString() {
    return String.format("%d:%d", line + 1, column);
  }

  public static TokenPosition dummyTokenPosition() {
    return new TokenPosition(-1, -1, -1);
  }
}
