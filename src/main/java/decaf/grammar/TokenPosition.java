package decaf.grammar;


public record TokenPosition(int line, int column, int offset) {

  @Override
  public String toString() {
    return String.format("%d:%d", line + 1, column);
  }
}
