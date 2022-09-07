package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.ControlFlowGraph;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.cfg.ControlFlowGraphASTVisitor;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.ir.DecafSemanticChecker;
import edu.mit.compilers.utils.DecafExceptionProcessor;
import edu.mit.compilers.utils.TarjanSCC;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;

public class DataFlowAnalysisTest {
    List<BasicBlock> basicBlockList;

    @Before
    public void setUp() {
        final String simpleForLoop = "void main() {int i; int a; for (i = 0; i < 10; i++) {a += i;}}";
        DecafExceptionProcessor decafExceptionProcessor = new DecafExceptionProcessor(simpleForLoop);
        DecafScanner scanner = new DecafScanner(simpleForLoop, decafExceptionProcessor);
        DecafParser parser = new DecafParser(scanner);
        parser.program();

        DecafSemanticChecker semChecker = new DecafSemanticChecker(parser.getRoot());
        semChecker.runChecks(decafExceptionProcessor);
        ControlFlowGraph cfgGenerator = new ControlFlowGraph(parser.getRoot(), semChecker.globalDescriptor);
        ControlFlowGraphASTVisitor visitor = cfgGenerator.build();
        basicBlockList = TarjanSCC.getReversePostOrder(visitor.methodNameToEntryBlock.get("main"));
    }

    @Test
    public void test_simpleForLoopReversePostOrderCorrect() {
        assertEquals(basicBlockList.size(), 4);
        assertEquals( 3,       basicBlockList.get(0).getAstNodes().size());
        assertEquals( "int i", basicBlockList.get(0).getAstNodes().get(0).getSourceCode());
        assertEquals( "int a", basicBlockList.get(0).getAstNodes().get(1).getSourceCode());
        assertEquals( "i = 0", basicBlockList.get(0).getAstNodes().get(2).getSourceCode());

        assertEquals( 2,        basicBlockList.get(1).getAstNodes().size());
        assertEquals( "a += i", basicBlockList.get(1).getAstNodes().get(0).getSourceCode());
        assertEquals( "i ++",   basicBlockList.get(1).getAstNodes().get(1).getSourceCode());

        assertEquals( 1,        basicBlockList.get(2).getAstNodes().size());
        assertEquals( "i < 10", basicBlockList.get(2).getAstNodes().get(0).getSourceCode());


        assertTrue(basicBlockList.get(3) instanceof NOP);
        assertEquals("exit_main", ((NOP) basicBlockList.get(3)).getNopLabel().orElseThrow());
    }
}