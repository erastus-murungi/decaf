package dataflow.ssapasses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import decaf.ir.dataflow.ssapasses.LatticeElement;


public class LatticeElementTest {
    LatticeElement c1, c2, c3;


    @BeforeEach
    protected void setUp()  {
        c1 = LatticeElement.constant(1L);
        c2 = LatticeElement.constant(1L);
        c3 = LatticeElement.constant(2L);
    }

    @Test
    public void testMeetWithFirstArgEqualsTop() {
        assertEquals(LatticeElement.meet(LatticeElement.top(), LatticeElement.top()), LatticeElement.top());
        assertEquals(LatticeElement.meet(LatticeElement.top(), c1), LatticeElement.top());
        assertEquals(LatticeElement.meet(LatticeElement.top(), LatticeElement.bottom()), LatticeElement.bottom());
    }

    @Test
    public void testMeetWithFirstArgEqualsConstant() {
        assertEquals(LatticeElement.meet(c1, LatticeElement.top()), LatticeElement.top());
        assertEquals(LatticeElement.meet(c1, c2), c1);
        assertEquals(LatticeElement.meet(c1, c3), LatticeElement.bottom());
        assertEquals(LatticeElement.meet(c1, LatticeElement.bottom()), LatticeElement.bottom());
    }

    @Test
    public void testMeetWithFirstArgEqualsBottom() {
        assertEquals(LatticeElement.meet(LatticeElement.bottom(), LatticeElement.top()), LatticeElement.bottom());
        assertEquals(LatticeElement.meet(LatticeElement.bottom(), c1), LatticeElement.bottom());
        assertEquals(LatticeElement.meet(LatticeElement.bottom(), LatticeElement.bottom()), LatticeElement.bottom());
    }

}