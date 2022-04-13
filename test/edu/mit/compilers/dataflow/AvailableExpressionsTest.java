package edu.mit.compilers.dataflow;

import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.codes.Triple;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.grammar.DecafScanner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class AvailableExpressionsTest {
    private final VariableName a = new VariableName("a");
    private final VariableName b = new VariableName("b");
    private final VariableName c = new VariableName("c");
    private final VariableName d = new VariableName("d");

    private Quadruple aEqualsBPlusC;
    private Quadruple dEqualsCPlusB;
    private Quadruple aEqualsBMinusC;
    private Quadruple dEqualsCMinusB;

    private Quadruple aEqualsBTimesC;
    private Quadruple dEqualsCTimesB;
    private Quadruple aEqualsBDivideC;
    private Quadruple dEqualsCDivideB;

    private Triple aEqualsMinusB;
    private Triple cEqualsMinusB;

    private final String[] allDecafOperators = {
            DecafScanner.PLUS,
            DecafScanner.MULTIPLY,
            DecafScanner.MINUS,
            DecafScanner.DIVIDE,
            DecafScanner.GEQ,
            DecafScanner.MOD,
            DecafScanner.GT,
            DecafScanner.LEQ,
            DecafScanner.LT
    };

    private final String[] commutativeOperators = {
            DecafScanner.PLUS,
            DecafScanner.MULTIPLY,
    };

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        aEqualsBPlusC = new Quadruple(a, b, DecafScanner.PLUS, c, null, null);
        dEqualsCPlusB = new Quadruple(d, c, DecafScanner.PLUS, b, null, null);
        aEqualsBMinusC = new Quadruple(a, b, DecafScanner.MINUS, c, null, null);
        dEqualsCMinusB = new Quadruple(d, c, DecafScanner.PLUS, b, null, null);

        aEqualsBTimesC = new Quadruple(a, b, DecafScanner.MULTIPLY, c, null, null);
        dEqualsCTimesB = new Quadruple(d, c, DecafScanner.MULTIPLY, b, null, null);
        aEqualsBDivideC = new Quadruple(a, b, DecafScanner.DIVIDE, c, null, null);
        dEqualsCDivideB = new Quadruple(d, c, DecafScanner.DIVIDE, b, null, null);

        aEqualsMinusB = new Triple(a, DecafScanner.MINUS, b, null);
        cEqualsMinusB = new Triple(c, DecafScanner.MINUS, b, null);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test_BinaryExpressionsAreIsomorphic_When_CommutativeExpressionsAreEqual() {
        // b + c == c + b
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(aEqualsBPlusC, dEqualsCPlusB));
        // b * c == c * b
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(aEqualsBTimesC, dEqualsCTimesB));
    }

    @Test
    public void test_BinaryExpressionsAreIsomorphic_When_NonCommutativeExpressionsAreEqual() {
        // b - c == b - c
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(aEqualsBMinusC, aEqualsBMinusC));
        // b / c == b / c
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(aEqualsBDivideC, aEqualsBDivideC));
    }

    @Test
    public void test_BinaryExpressionsAreNotIsomorphic_When_NonCommutativeExpressionsAreNotEqual() {
        // b - c != c - b
        assertFalse(AvailableExpressions.expressionsAreIsomorphic(aEqualsBMinusC, dEqualsCMinusB));
        // b / c != c / b
        assertFalse(AvailableExpressions.expressionsAreIsomorphic(aEqualsBDivideC, dEqualsCDivideB));
    }

    @Test
    public void test_IllegalArgumentExceptionThrown_When_FirstArgIsNull() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("first assignment is a null pointer");
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(null, cEqualsMinusB));
    }

    @Test
    public void test_IllegalArgumentExceptionThrown_When_SecondArgIsNull() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("second assignment is a null pointer");
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(cEqualsMinusB, null));
    }

    @Test
    public void test_UnaryExpressions_AreIsomorphic_When_Equal() {
        // -b == -b
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(aEqualsMinusB, cEqualsMinusB));
    }

    @Test
    public void test_Only_MultiplyAndAdd_AreCommutative() {
        int numCommutative = 0;
        for (String operator: allDecafOperators)
            numCommutative += (AvailableExpressions.operatorIsCommutative(operator) ? 1 : 0);
        assertEquals(commutativeOperators.length, numCommutative);
        for (String operator: commutativeOperators)
            assertTrue(AvailableExpressions.operatorIsCommutative(operator));
    }
}