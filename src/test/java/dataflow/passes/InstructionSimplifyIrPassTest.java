package dataflow.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import decaf.common.Compilation;
import decaf.common.Utils;
import decaf.ast.AST;
import decaf.ast.Expression;
import decaf.ast.LocationVariable;
import decaf.dataflow.passes.InstructionSimplifyIrPass;

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
        assertEquals("10", expressions.remove(0)
                .getSourceCode());
    }

    @Test
    public void matchBinOpOperandsCommutative() {
    }

    @Test
    public void matchBinOpOperands() {
    }
}