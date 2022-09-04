package edu.mit.compilers.dataflow.passes;

import static edu.mit.compilers.grammar.DecafScanner.EQ;
import static edu.mit.compilers.grammar.DecafScanner.MULTIPLY;
import static edu.mit.compilers.grammar.DecafScanner.PLUS;

import java.util.ArrayList;
import java.util.Set;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.utils.Utils;

public class InstructionSimplifyPass extends OptimizationPass {
    static final NumericalConstant mZero = new NumericalConstant(0L, Type.Int);
    static final NumericalConstant mOne = new NumericalConstant(1L, Type.Int);

    private static NumericalConstant getZero() {
        return new NumericalConstant(0L, Type.Bool);
    }

    private static NumericalConstant getOne() {
        return new NumericalConstant(1L, Type.Int);
    }

    public static boolean matchBinOpOperandsCommutative(BinaryInstruction instruction,
                                                        Value lhsExpected,
                                                        Value rhsExpected) {
        return matchBinOpOperands(instruction, lhsExpected, rhsExpected) ||
                matchBinOpOperands(instruction, rhsExpected, lhsExpected);
    }

    public static boolean matchBinOpOperands(BinaryInstruction instruction,
                                             Value lhsExpected,
                                             Value rhsExpected) {
        var lhsActual = instruction.fstOperand;
        var rhsActual = instruction.sndOperand;
        return lhsActual
                .equals(lhsExpected) &&
                rhsActual
                        .equals(rhsExpected);
    }

    public InstructionSimplifyPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    private static Value getNotEq(BinaryInstruction binaryInstruction, Value expected) {
        if (binaryInstruction.fstOperand.equals(expected))
            return binaryInstruction.sndOperand;
        return binaryInstruction.fstOperand;
    }

    private static Value getNonZero(BinaryInstruction binaryInstruction) {
        return getNotEq(binaryInstruction, mZero);
    }

    private static Instruction trySimplifyAddInstruction(BinaryInstruction addInstruction) {
        assert addInstruction.operator.equals(PLUS);
        // 0 + X -> X
        var X = getNonZero(addInstruction);
        if (matchBinOpOperandsCommutative(addInstruction, mZero, X)) {
            return CopyInstruction.noMetaData(addInstruction.getDestination(), X);
        }
        return addInstruction;
    }

    private static Instruction trySimplifyEqInstruction(BinaryInstruction eqInstruction) {
        assert eqInstruction.operator.equals(EQ);
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
        assert multiplyInstruction.operator.equals(MULTIPLY);

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
            return CopyInstruction.noMetaData(binaryInstruction.getDestination(), new NumericalConstant(aLong.get(), Type.Int));
        }
        Instruction newTac = null;
        switch (binaryInstruction.operator) {
            case PLUS: {
                newTac = trySimplifyAddInstruction(binaryInstruction);
                break;
            }
            case MULTIPLY: {
                newTac = trySimplifyMulInstruction(binaryInstruction);
                break;
            }
            case EQ: {
                newTac = trySimplifyEqInstruction(binaryInstruction);
                break;
            }
        }

        if (newTac != null)
            return newTac;
        return binaryInstruction;
    }

    private Instruction simplifyTriple(UnaryInstruction unaryInstruction) {
        return unaryInstruction;
    }


    private void simplifyInstructions() {
        for (BasicBlock basicBlock : basicBlocks) {
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
