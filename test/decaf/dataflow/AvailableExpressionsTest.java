package decaf.dataflow;

import static org.junit.Assert.assertThrows;

import junit.framework.TestCase;

import org.junit.Before;

import decaf.ast.Type;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.names.IrRegister;
import decaf.dataflow.analyses.AvailableExpressions;
import decaf.grammar.DecafScanner;
import decaf.codegen.codes.BinaryInstruction;

public class AvailableExpressionsTest extends TestCase {
  private static final String[] allDecafOperators = {DecafScanner.PLUS, DecafScanner.MULTIPLY, DecafScanner.MINUS, DecafScanner.DIVIDE, DecafScanner.GEQ, DecafScanner.MOD, DecafScanner.GT, DecafScanner.LEQ, DecafScanner.LT};
  private static final String[] commutativeOperators = {DecafScanner.PLUS, DecafScanner.MULTIPLY,};
  private final IrRegister a = new IrRegister(
      "a",
      Type.Int
  );
  private final IrRegister b = new IrRegister(
      "b",
      Type.Int
  );
  private final IrRegister c = new IrRegister(
      "c",
      Type.Int
  );
  private final IrRegister d = new IrRegister(
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

  @Before
  public void setUp() {
    aEqualsBPlusC = new BinaryInstruction(
        a,
        b,
        DecafScanner.PLUS,
        c,
        null,
        null
    );
    dEqualsCPlusB = new BinaryInstruction(
        d,
        c,
        DecafScanner.PLUS,
        b,
        null,
        null
    );
    aEqualsBMinusC = new BinaryInstruction(
        a,
        b,
        DecafScanner.MINUS,
        c,
        null,
        null
    );
    dEqualsCMinusB = new BinaryInstruction(
        d,
        c,
        DecafScanner.PLUS,
        b,
        null,
        null
    );

    aEqualsBTimesC = new BinaryInstruction(
        a,
        b,
        DecafScanner.MULTIPLY,
        c,
        null,
        null
    );
    dEqualsCTimesB = new BinaryInstruction(
        d,
        c,
        DecafScanner.MULTIPLY,
        b,
        null,
        null
    );
    aEqualsBDivideC = new BinaryInstruction(
        a,
        b,
        DecafScanner.DIVIDE,
        c,
        null,
        null
    );
    dEqualsCDivideB = new BinaryInstruction(
        d,
        c,
        DecafScanner.DIVIDE,
        b,
        null,
        null
    );

    aEqualsMinusB = new UnaryInstruction(
        a,
        DecafScanner.MINUS,
        b,
        null
    );
    cEqualsMinusB = new UnaryInstruction(
        c,
        DecafScanner.MINUS,
        b,
        null
    );
  }

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

  public void test_UnaryExpressions_AreIsomorphic_When_Equal() {
    // -b == -b
    assertTrue(AvailableExpressions.expressionsAreIsomorphic(
        aEqualsMinusB,
        cEqualsMinusB
    ));
  }

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