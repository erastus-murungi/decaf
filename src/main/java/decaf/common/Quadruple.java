package decaf.common;

import java.util.Objects;

public class Quadruple<T1, T2, T3, T4> {
  T1 first;
  T2 second;
  T3 third;
  T4 fourth;

  public Quadruple(
      T1 first,
      T2 second,
      T3 third,
      T4 fourth
  ) {
    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;
  }

  public T1 first() {
    return this.first;
  }

  public T2 second() {
    return this.second;
  }

  public T3 third() {
    return this.third;
  }

  public T4 fourth() {
    return this.fourth;
  }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ", " + third + ", " + fourth + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Quadruple<?, ?, ?, ?> quadruple)) return false;
    return Objects.equals(
        first,
        quadruple.first
    ) && Objects.equals(
        second,
        quadruple.second
    ) && Objects.equals(
        third,
        quadruple.third
    ) && Objects.equals(
        fourth,
        quadruple.fourth
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        first,
        second,
        third,
        fourth
    );
  }
}
