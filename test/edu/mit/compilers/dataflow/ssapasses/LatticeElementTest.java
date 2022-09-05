package edu.mit.compilers.dataflow.ssapasses;

import junit.framework.TestCase;


public class LatticeElementTest extends TestCase {
    LatticeElement c1, c2, c3;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        c1 = LatticeElement.constant(1L);
        c2 = LatticeElement.constant(1L);
        c3 = LatticeElement.constant(2L);
    }

    public void testMeetWithFirstArgEqualsTop() {
        assertEquals(LatticeElement.meet(LatticeElement.top(), LatticeElement.top()), LatticeElement.top());
        assertEquals(LatticeElement.meet(LatticeElement.top(), c1), LatticeElement.top());
        assertEquals(LatticeElement.meet(LatticeElement.top(), LatticeElement.bottom()), LatticeElement.bottom());
    }

    public void testMeetWithFirstArgEqualsConstant() {
        assertEquals(LatticeElement.meet(c1, LatticeElement.top()), LatticeElement.top());
        assertEquals(LatticeElement.meet(c1, c2), c1);
        assertEquals(LatticeElement.meet(c1, c3), LatticeElement.bottom());
        assertEquals(LatticeElement.meet(c1, LatticeElement.bottom()), LatticeElement.bottom());
    }

    public void testMeetWithFirstArgEqualsBottom() {
        assertEquals(LatticeElement.meet(LatticeElement.bottom(), LatticeElement.top()), LatticeElement.bottom());
        assertEquals(LatticeElement.meet(LatticeElement.bottom(), c1), LatticeElement.bottom());
        assertEquals(LatticeElement.meet(LatticeElement.bottom(), LatticeElement.bottom()), LatticeElement.bottom());
    }

}