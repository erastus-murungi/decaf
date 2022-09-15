package decaf.dataflow;

import decaf.cfg.NOP;
import decaf.common.DecafExceptionProcessor;
import decaf.common.TarjanSCC;
import decaf.grammar.DecafParser;
import decaf.grammar.DecafScanner;
import decaf.ir.SemanticCheckingManager;
import decaf.cfg.BasicBlock;
import decaf.cfg.ControlFlowGraph;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;

public class DataFlowAnalysisTest {
    List<BasicBlock> basicBlockList;

    @Before
    public void setUp() {
        final var simpleForLoop = "void main() {int i; int a; for (i = 0; i < 10; i++) {a += i;}}";
        var decafExceptionProcessor = new DecafExceptionProcessor(simpleForLoop);
        var scanner = new DecafScanner(simpleForLoop, decafExceptionProcessor);
        var parser = new DecafParser(scanner);
        parser.program();

        var semChecker = new SemanticCheckingManager(parser.getRoot());
        semChecker.runChecks(decafExceptionProcessor);
        ControlFlowGraph cfgGenerator = new ControlFlowGraph(parser.getRoot(),
                                                             semChecker.getGlobalDescriptor()
        );
        cfgGenerator.build();
        basicBlockList = TarjanSCC.getReversePostOrder(cfgGenerator.getMethodNameToEntryBlock().get("main"));
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