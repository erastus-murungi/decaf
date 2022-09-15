package decaf.dataflow.passes;

import java.util.ArrayList;

import decaf.ast.Type;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.UnaryInstruction;
import decaf.common.Utils;
import decaf.dataflow.OptimizationContext;
import decaf.grammar.DecafScanner;
import decaf.cfg.BasicBlock;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.Method;
import decaf.codegen.names.IrValue;
import decaf.codegen.names.IrIntegerConstant;

public class InstructionSimplifyPass extends OptimizationPass {
    static final IrIntegerConstant mZero = new IrIntegerConstant(0L, Type.Int);
    static final IrIntegerConstant mOne = new IrIntegerConstant(1L, Type.Int);

    public InstructionSimplifyPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    private static IrIntegerConstant getZero() {
        return new IrIntegerConstant(0L, Type.Bool);
    }

    public static boolean matchBinOpOperandsCommutative(BinaryInstruction instruction,
                                                        IrValue lhsExpected,
                                                        IrValue rhsExpected) {
        return matchBinOpOperands(instruction, lhsExpected, rhsExpected) ||
                matchBinOpOperands(instruction, rhsExpected, lhsExpected);
    }

    public static boolean matchBinOpOperands(BinaryInstruction instruction,
                                             IrValue lhsExpected,
                                             IrValue rhsExpected) {
        var lhsActual = instruction.fstOperand;
        var rhsActual = instruction.sndOperand;
        return lhsActual
                .equals(lhsExpected) &&
                rhsActual
                        .equals(rhsExpected);
    }

    private static IrValue getNotEq(BinaryInstruction binaryInstruction, IrValue expected) {
        if (binaryInstruction.fstOperand.equals(expected))
            return binaryInstruction.sndOperand;
        return binaryInstruction.fstOperand;
    }

    private static IrValue getNonZero(BinaryInstruction binaryInstruction) {
        return getNotEq(binaryInstruction, mZero);
    }

    private static Instruction trySimplifyAddInstruction(BinaryInstruction addInstruction) {
        assert addInstruction.operator.equals(DecafScanner.PLUS);
        // 0 + X -> X
        var X = getNonZero(addInstruction);
        if (matchBinOpOperandsCommutative(addInstruction, mZero, X)) {
            return CopyInstruction.noMetaData(addInstruction.getDestination(), X);
        }
        return addInstruction;
    }

    private static Instruction trySimplifyEqInstruction(BinaryInstruction eqInstruction) {
        assert eqInstruction.operator.equals(DecafScanner.EQ);
        // X == true -> X
        // true == X -> X
        var X = getNotEq(eqInstruction, mOne);
        if (X.getType().equals(Type.Bool)) {
            if (matchBinOpOperandsCommutative(eqInstruction, X, mOne))
                return CopyInstruction.noMetaData(eqInstruction.getDestination(), X);
            // true == true -> true
            if (matchBinOpOperands(eqInstruction, mOne, mOne)) {
                return CopyInstruction.noMetaData(eqInstruction.getDestination(), mOne);
            }
            // true == false -> false
            // false == true -> false
            if (matchBinOpOperandsCommutative(eqInstruction, mOne, mZero)) {
                return CopyInstruction.noMetaData(eqInstruction.getDestination(), getZero());
            }
        }
        return eqInstruction;
    }

    private static Instruction trySimplifyMulInstruction(BinaryInstruction multiplyInstruction) {
        assert multiplyInstruction.operator.equals(DecafScanner.MULTIPLY);

        // 0 * X -> 0
        // X * 0 -> 0
        final var X = getNonZero(multiplyInstruction);
        if (matchBinOpOperandsCommutative(multiplyInstruction, mZero, X)) {
            return CopyInstruction.noMetaData(multiplyInstruction.getDestination(), getZero());
        }
        // X * 1 -> X
        // 1 * X -> X
        if (matchBinOpOperandsCommutative(multiplyInstruction, mOne, X)) {
            return CopyInstruction.noMetaData(multiplyInstruction.getDestination(), X);
        }
        return multiplyInstruction;
    }

    private Instruction simplifyQuadruple(BinaryInstruction binaryInstruction) {
        var aLong = Utils.symbolicallyEvaluate(String.format("%s %s %s", binaryInstruction.fstOperand.getLabel(), binaryInstruction.operator, binaryInstruction.sndOperand.getLabel()));
        if (aLong.isPresent()) {
            return CopyInstruction.noMetaData(binaryInstruction.getDestination(), new IrIntegerConstant(aLong.get(), Type.Int));
        }
        Instruction newTac = null;
        switch (binaryInstruction.operator) {
            case DecafScanner.PLUS -> newTac = trySimplifyAddInstruction(binaryInstruction);
            case DecafScanner.MULTIPLY -> newTac = trySimplifyMulInstruction(binaryInstruction);
            case DecafScanner.EQ -> newTac = trySimplifyEqInstruction(binaryInstruction);
        }

        if (newTac != null)
            return newTac;
        return binaryInstruction;
    }

    private Instruction simplifyTriple(UnaryInstruction unaryInstruction) {
        return unaryInstruction;
    }


    private void simplifyInstructions() {
        for (BasicBlock basicBlock : getBasicBlocksList()) {
            var newTacList = new ArrayList<Instruction>();
            for (var tac : basicBlock.getInstructionList()) {
                Instruction newTac;
                if (tac instanceof BinaryInstruction) {
                    newTac = simplifyQuadruple((BinaryInstruction) tac);
                } else if (tac instanceof UnaryInstruction) {
                    newTac = simplifyTriple((UnaryInstruction) tac);
                } else {
                    newTac = tac;
                }
                newTacList.add(newTac);
            }
            basicBlock.getInstructionList().reset(newTacList);
        }
    }

    @Override
    public boolean runFunctionPass() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        simplifyInstructions();
        return !oldCodes.equals(entryBlock.getInstructionList());
    }
}
