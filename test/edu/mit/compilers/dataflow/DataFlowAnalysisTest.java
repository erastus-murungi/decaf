package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.CFGGenerator;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.cfg.iCFGVisitor;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.ir.DecafSemanticChecker;
import edu.mit.compilers.utils.DecafExceptionProcessor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;

public class DataFlowAnalysisTest {
    List<BasicBlock> basicBlockList;

    @Before
    public void setUp() throws Exception {
        final String simpleForLoop = "void main() {int i; int a; for (i = 0; i < 10; i++) {a += i;}}";
        DecafExceptionProcessor decafExceptionProcessor = new DecafExceptionProcessor(simpleForLoop);
        DecafScanner scanner = new DecafScanner(simpleForLoop, decafExceptionProcessor);
        DecafParser parser = new DecafParser(scanner);
        parser.program();

        DecafSemanticChecker semChecker = new DecafSemanticChecker(parser.getRoot());
        semChecker.runChecks(decafExceptionProcessor);
        CFGGenerator cfgGenerator = new CFGGenerator(parser.getRoot(), semChecker.globalDescriptor);
        iCFGVisitor visitor = cfgGenerator.buildiCFG();
        basicBlockList = DataFlowAnalysis.getReversePostOrder(visitor.methodCFGBlocks.get("main"));
    }

    @Test
    public void test_simpleForLoopReversePostOrderCorrect() {
        assertEquals(basicBlockList.size(), 4);
        assertEquals( 3,       basicBlockList.get(0).lines.size());
        assertEquals( "int i", basicBlockList.get(0).lines.get(0).getSourceCode());
        assertEquals( "int a", basicBlockList.get(0).lines.get(1).getSourceCode());
        assertEquals( "i = 0", basicBlockList.get(0).lines.get(2).getSourceCode());

        assertEquals( 2,        basicBlockList.get(1).lines.size());
        assertEquals( "a += i", basicBlockList.get(1).lines.get(0).getSourceCode());
        assertEquals( "i ++",   basicBlockList.get(1).lines.get(1).getSourceCode());

        assertEquals( 1,        basicBlockList.get(2).lines.size());
        assertEquals( "i < 10", basicBlockList.get(2).lines.get(0).getSourceCode());


        assertTrue(basicBlockList.get(3) instanceof NOP);
        assertEquals("Exit main", ((NOP) basicBlockList.get(3)).getNopLabel().orElseThrow());
    }
}