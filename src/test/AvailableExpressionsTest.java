package tests;

import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.dataflow.AvailableExpressions;
import edu.mit.compilers.grammar.DecafScanner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

    @Before
    public void setUp() throws Exception {
        aEqualsBPlusC = new Quadruple(a, b, DecafScanner.PLUS, c, null, null);
        dEqualsCPlusB = new Quadruple(d, c, DecafScanner.PLUS, b, null, null);
        aEqualsBMinusC = new Quadruple(a, b, DecafScanner.MINUS, c, null, null);
        dEqualsCMinusB = new Quadruple(d, c, DecafScanner.PLUS, b, null, null);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testExpressionsAreIsomorphicReturnsTrueWhenTwoCommutativeExpressionsAreEqual() {
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(aEqualsBPlusC, dEqualsCPlusB));
    }

    @Test
    public void testExpressionsAreIsomorphicReturnsTrueWhenTwoNonCommutativeExpressionsAreEqual() {
        assertTrue(AvailableExpressions.expressionsAreIsomorphic(aEqualsBMinusC, aEqualsBMinusC));
    }

    @Test
    public void testExpressionsAreIsomorphicReturnsTrueWhenTwoNonCommutativeExpressionsAreNotEqual() {
        assertFalse(AvailableExpressions.expressionsAreIsomorphic(aEqualsBMinusC, dEqualsCMinusB));
    }
}