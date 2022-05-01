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
import edu.mit.compilers.utils.Compilation;
import edu.mit.compilers.utils.Utils;

public class InstructionSimplifyIrPassTest {
    @Test
    public void simplifyExpression() throws FileNotFoundException {
        Compilation compilation = new Compilation("void main() {int x; x = (1 + 6 * 3 / 2);}", false);
        compilation.run();
        AST astRoot = compilation.getAstRoot();
        List<Expression> expressions = new ArrayList<>(Utils.findAllOfType(astRoot, Expression.class));
        InstructionSimplifyIrPass.run(astRoot);
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