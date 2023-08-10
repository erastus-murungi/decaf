package dataflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import decaf.ast.Type;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.names.IrSsaRegister;
import decaf.dataflow.analyses.AvailableExpressions;
import decaf.grammar.Scanner;
import decaf.codegen.codes.BinaryInstruction;

public class AvailableExpressionsTest {
  private static final String[] allDecafOperators = {Scanner.PLUS, Scanner.MULTIPLY, Scanner.MINUS, Scanner.DIVIDE, Scanner.GEQ, Scanner.MOD, Scanner.GT, Scanner.LEQ, Scanner.LT};
  private static final String[] commutativeOperators = {Scanner.PLUS, Scanner.MULTIPLY,};
  private final IrSsaRegister a = new IrSsaRegister(
      "a",
      Type.Int
  );
  private final IrSsaRegister b = new IrSsaRegister(
      "b",
      Type.Int
  );
  private final IrSsaRegister c = new IrSsaRegister(
      "c",
      Type.Int
  );
  private final IrSsaRegister d = new IrSsaRegister(
      "d",
      Type.Int
  );
  private BinaryInstruction aEqualsBPlusC;
  private BinaryInstruction dEqualsCPlusB;
  private BinaryInstruction aEqualsBMinusC;
  private BinaryInstruction dEqualsCMinusB;
  private BinaryInstruction aEqualsBTimesC;
  private BinaryInstruction dEqualsCTimesB;
  private BinaryInstruction aEqualsBDivideC;
  private BinaryInstruction dEqualsCDivideB;
  private UnaryInstruction aEqualsMinusB;
  private UnaryInstruction cEqualsMinusB;

  @BeforeEach
  public void setUp() {
    aEqualsBPlusC = new BinaryInstruction(
        a,
        b,
        Scanner.PLUS,
        c,
        null,
        null
    );
    dEqualsCPlusB = new BinaryInstruction(
        d,
        c,
        Scanner.PLUS,
        b,
        null,
        null
    );
    aEqualsBMinusC = new BinaryInstruction(
        a,
        b,
        Scanner.MINUS,
        c,
        null,
        null
    );
    dEqualsCMinusB = new BinaryInstruction(
        d,
        c,
        Scanner.PLUS,
        b,
        null,
        null
    );

    aEqualsBTimesC = new BinaryInstruction(
        a,
        b,
        Scanner.MULTIPLY,
        c,
        null,
        null
    );
    dEqualsCTimesB = new BinaryInstruction(
        d,
        c,
        Scanner.MULTIPLY,
        b,
        null,
        null
    );
    aEqualsBDivideC = new BinaryInstruction(
        a,
        b,
        Scanner.DIVIDE,
        c,
        null,
        null
    );
    dEqualsCDivideB = new BinaryInstruction(
        d,
        c,
        Scanner.DIVIDE,
        b,
        null,
        null
    );

    aEqualsMinusB = new UnaryInstruction(
        a,
        Scanner.MINUS,
        b,
        null
    );
    cEqualsMinusB = new UnaryInstruction(
        c,
        Scanner.MINUS,
        b,
        null
    );
  }

  @Test
  public void test_BinaryExpressionsAreIsomorphic_When_CommutativeExpressionsAreEqual() {
    // b + c == c + b
    assertTrue(AvailableExpressions.expressionsAreIsomorphic(
        aEqualsBPlusC,
        dEqualsCPlusB
    ));
    // b * c == c * b
    assertTrue(AvailableExpressions.expressionsAreIsomorphic(
        aEqualsBTimesC,
        dEqualsCTimesB
    ));
  }

  @Test
  public void test_BinaryExpressionsAreIsomorphic_When_NonCommutativeExpressionsAreEqual() {
    // b - c == b - c
    assertTrue(AvailableExpressions.expressionsAreIsomorphic(
        aEqualsBMinusC,
        aEqualsBMinusC
    ));
    // b / c == b / c
    assertTrue(AvailableExpressions.expressionsAreIsomorphic(
        aEqualsBDivideC,
        aEqualsBDivideC
    ));
  }

  @Test
  public void test_BinaryExpressionsAreNotIsomorphic_When_NonCommutativeExpressionsAreNotEqual() {
    // b - c != c - b
    assertFalse(AvailableExpressions.expressionsAreIsomorphic(
        aEqualsBMinusC,
        dEqualsCMinusB
    ));
    // b / c != c / b
    assertFalse(AvailableExpressions.expressionsAreIsomorphic(
        aEqualsBDivideC,
        dEqualsCDivideB
    ));
  }

  @Test
  public void test_IllegalArgumentExceptionThrown_When_FirstArgIsNull() {
    IllegalArgumentException illegalArgumentException = assertThrows(
        IllegalArgumentException.class,
        () -> AvailableExpressions.expressionsAreIsomorphic(
            null,
            cEqualsMinusB
        )
    );
    assertTrue(illegalArgumentException.getMessage()
                                       .contains("first assignment is a null pointer"));
  }

  @Test
  public void test_IllegalArgumentExceptionThrown_When_SecondArgIsNull() {
    IllegalArgumentException illegalArgumentException = assertThrows(
        IllegalArgumentException.class,
        () -> AvailableExpressions.expressionsAreIsomorphic(
            cEqualsMinusB,
            null
        )
    );
    assertTrue(illegalArgumentException.getMessage()
                                       .contains("second assignment is a null pointer"));
  }

  @Test
  public void test_UnaryExpressions_AreIsomorphic_When_Equal() {
    // -b == -b
    assertTrue(AvailableExpressions.expressionsAreIsomorphic(
        aEqualsMinusB,
        cEqualsMinusB
    ));
  }

  @Test
  public void test_Only_MultiplyAndAdd_AreCommutative() {
    int numCommutative = 0;
    for (String operator : allDecafOperators)
      numCommutative += (AvailableExpressions.operatorIsCommutative(operator) ? 1: 0);
    assertEquals(
        commutativeOperators.length,
        numCommutative
    );
    for (String operator : commutativeOperators)
      assertTrue(AvailableExpressions.operatorIsCommutative(operator));
  }
}