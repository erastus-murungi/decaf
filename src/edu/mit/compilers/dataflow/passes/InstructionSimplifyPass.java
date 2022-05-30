package edu.mit.compilers.dataflow.passes;

import static edu.mit.compilers.grammar.DecafScanner.EQ;
import static edu.mit.compilers.grammar.DecafScanner.MULTIPLY;
import static edu.mit.compilers.grammar.DecafScanner.PLUS;

import java.util.ArrayList;
import java.util.Set;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;

public class InstructionSimplifyPass extends OptimizationPass {
    static final ConstantName mZero = new ConstantName(0L, Type.Int);
    static final ConstantName mOne = new ConstantName(1L, Type.Int);

    private static ConstantName getZero() {
        return new ConstantName(0L, Type.Bool);
    }

    private static ConstantName getOne() {
        return new ConstantName(1L, Type.Int);
    }

    public static boolean matchBinOpOperandsCommutative(BinaryInstruction instruction,
                                                        AbstractName lhsExpected,
                                                        AbstractName rhsExpected) {
        return matchBinOpOperands(instruction, lhsExpected, rhsExpected) ||
                matchBinOpOperands(instruction, rhsExpected, lhsExpected);
    }

    public static boolean matchBinOpOperands(BinaryInstruction instruction,
                                             AbstractName lhsExpected,
                                             AbstractName rhsExpected) {
        var lhsActual = instruction.fstOperand;
        var rhsActual = instruction.sndOperand;
        return lhsActual
                .equals(lhsExpected) &&
                rhsActual
                        .equals(rhsExpected);
    }

    public InstructionSimplifyPass(Set<AbstractName> globalVariables, Method method) {
        super(globalVariables, method);
    }

    private static AbstractName getNotEq(BinaryInstruction binaryInstruction, AbstractName expected) {
        if (binaryInstruction.fstOperand.equals(expected))
            return binaryInstruction.sndOperand;
        return binaryInstruction.fstOperand;
    }

    private static AbstractName getNonZero(BinaryInstruction binaryInstruction) {
        return getNotEq(binaryInstruction, mZero);
    }

    private static Instruction trySimplifyAddInstruction(BinaryInstruction addInstruction) {
        assert addInstruction.operator.equals(PLUS);
        // 0 + X -> X
        var X = getNonZero(addInstruction);
        if (matchBinOpOperandsCommutative(addInstruction, mZero, X)) {
            return Assign.ofRegularAssign(addInstruction.getStore(), X);
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
                return Assign.ofRegularAssign(eqInstruction.getStore(), X);
            // true == true -> true
            if (matchBinOpOperands(eqInstruction, mOne, mOne)) {
                return Assign.ofRegularAssign(eqInstruction.getStore(), mOne);
            }
            // true == false -> false
            // false == true -> false
            if (matchBinOpOperandsCommutative(eqInstruction, mOne, mZero)) {
                return Assign.ofRegularAssign(eqInstruction.getStore(), getZero());
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
            return Assign.ofRegularAssign(multiplyInstruction.getStore(), getZero());
        }
        // X * 1 -> X
        // 1 * X -> X
        if (matchBinOpOperandsCommutative(multiplyInstruction, mOne, X)) {
            return Assign.ofRegularAssign(multiplyInstruction.getStore(), X);
        }
        return multiplyInstruction;
    }

    private Instruction simplifyQuadruple(BinaryInstruction binaryInstruction) {
        var aLong = InstructionSimplifyIrPass.symbolicallyEvaluate(String.format("%s %s %s", binaryInstruction.fstOperand.getLabel(), binaryInstruction.operator, binaryInstruction.sndOperand.getLabel()));
        if (aLong.isPresent()) {
            return Assign.ofRegularAssign(binaryInstruction.getStore(), new ConstantName(aLong.get(), Type.Int));
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
    public boolean run() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        simplifyInstructions();
        return !oldCodes.equals(entryBlock.getInstructionList());
    }
}
