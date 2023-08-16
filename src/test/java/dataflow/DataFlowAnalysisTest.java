package dataflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import decaf.cfg.NOP;
import decaf.common.CompilationContext;
import decaf.common.StronglyConnectedComponentsTarjan;
import decaf.grammar.Parser;
import decaf.grammar.Scanner;
import decaf.ir.SemanticChecker;
import decaf.cfg.BasicBlock;
import decaf.cfg.ControlFlowGraph;


import java.util.List;

public class DataFlowAnalysisTest {
    List<BasicBlock> basicBlockList;

    @BeforeEach
    public void setUp() {
        final var simpleForLoop = "void main() {int i; int a; for (i = 0; i < 10; i++) {a += i;}}";
        var context = new CompilationContext(simpleForLoop);
        var scanner = new Scanner(simpleForLoop, context);
        var parser = new Parser(scanner, context);
        parser.program();

        var semChecker = new SemanticChecker(parser.getRoot());
        semChecker.runChecks(context);
        ControlFlowGraph cfgGenerator = new ControlFlowGraph(parser.getRoot(),
                                                             semChecker.getGlobalDescriptor()
        );
        cfgGenerator.build();
        basicBlockList = StronglyConnectedComponentsTarjan.getReversePostOrder(cfgGenerator.getMethodNameToEntryBlockMapping().get("main"));
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