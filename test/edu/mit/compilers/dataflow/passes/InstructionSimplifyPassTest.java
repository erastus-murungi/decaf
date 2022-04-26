package edu.mit.compilers.dataflow.passes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.ExprContext;
import edu.mit.compilers.ast.Expression;
import edu.mit.compilers.ast.LocationVariable;
import edu.mit.compilers.utils.CompilationController;
import edu.mit.compilers.utils.Utils;

public class InstructionSimplifyPassTest {
    @Test
    public void simplifyExpression() throws FileNotFoundException {
        CompilationController compilationController = new CompilationController("void main() {int x; x = (1 + 6 * 3 / 2);}");
        compilationController.run();
        AST astRoot = compilationController.getAstRoot();
        List<Expression> expressions = new ArrayList<>(Utils.findAllOfType(astRoot, Expression.class));
        InstructionSimplifyPass.run(astRoot);
        expressions = expressions.stream().filter(expression -> !(expression instanceof LocationVariable &&
                ((LocationVariable) expression).name.context.equals(ExprContext.STORE))).collect(Collectors.toList());
        assert expressions.size() == 1;
        assertEquals("(10)", expressions.remove(0)
                .getSourceCode());
    }

    @Test
    public void matchBinOpOperandsCommutative() {
    }

    @Test
    public void matchBinOpOperands() {
    }
}