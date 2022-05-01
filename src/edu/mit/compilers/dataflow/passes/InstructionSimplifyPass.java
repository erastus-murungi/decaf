package edu.mit.compilers.dataflow.passes;

import static edu.mit.compilers.grammar.DecafScanner.EQ;
import static edu.mit.compilers.grammar.DecafScanner.MULTIPLY;
import static edu.mit.compilers.grammar.DecafScanner.PLUS;

import java.util.ArrayList;
import java.util.Set;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.codes.Triple;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;

public class InstructionSimplifyPass extends OptimizationPass {
    private static final ConstantName mZero = new ConstantName(0L, BuiltinType.Int);
    private static final ConstantName mOne = new ConstantName(1L, BuiltinType.Int);

    private static ConstantName getZero() {
        return new ConstantName(0L, BuiltinType.Int);
    }

    private static ConstantName getOne() {
        return new ConstantName(1L, BuiltinType.Int);
    }

    public static boolean matchBinOpOperandsCommutative(Quadruple instruction,
                                                        AbstractName lhsExpected,
                                                        AbstractName rhsExpected) {
        return matchBinOpOperands(instruction, lhsExpected, rhsExpected) ||
                matchBinOpOperands(instruction, rhsExpected, lhsExpected);
    }

    public static boolean matchBinOpOperands(Quadruple instruction,
                                             AbstractName lhsExpected,
                                             AbstractName rhsExpected) {
        var lhsActual = instruction.fstOperand;
        var rhsActual = instruction.sndOperand;
        return lhsActual
                .equals(lhsExpected) &&
                rhsActual
                        .equals(rhsExpected);
    }

    public InstructionSimplifyPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    private static AbstractName getNotEq(Quadruple quadruple, AbstractName expected) {
        if (quadruple.fstOperand.equals(expected))
            return quadruple.sndOperand;
        return quadruple.fstOperand;
    }

    private static AbstractName getNonZero(Quadruple quadruple) {
        return getNotEq(quadruple, mZero);
    }

    private static ThreeAddressCode trySimplifyAddInstruction(Quadruple addInstruction) {
        assert addInstruction.operator.equals(PLUS);
        // 0 + X -> X
        var X = getNonZero(addInstruction);
        if (matchBinOpOperandsCommutative(addInstruction, mZero, X)) {
            return Assign.ofRegularAssign(addInstruction.dst, X);
        }
        return addInstruction;
    }

    private static ThreeAddressCode trySimplifyEqInstruction(Quadruple eqInstruction) {
        assert eqInstruction.operator.equals(EQ);
        // X == true -> X
        // true == X -> X
        var X = getNotEq(eqInstruction, mOne);
        if (matchBinOpOperandsCommutative(eqInstruction, X, mOne))
            return Assign.ofRegularAssign(eqInstruction.dst, X);
        // true == true -> true
        if (matchBinOpOperands(eqInstruction, mOne, mOne)) {
            return Assign.ofRegularAssign(eqInstruction.dst, mOne);
        }
        // true == false -> false
        // false == true -> false
        if (matchBinOpOperandsCommutative(eqInstruction, mOne, mZero)) {
            return Assign.ofRegularAssign(eqInstruction.dst, getZero());
        }
        return eqInstruction;
    }

    private static ThreeAddressCode trySimplifyMulInstruction(Quadruple multiplyInstruction) {
        assert multiplyInstruction.operator.equals(MULTIPLY);

        // 0 * X -> 0
        // X * 0 -> 0
        final var X = getNonZero(multiplyInstruction);
        if (matchBinOpOperandsCommutative(multiplyInstruction, mZero, X)) {
            return Assign.ofRegularAssign(multiplyInstruction.dst, getZero());
        }
        // X * 1 -> X
        // 1 * X -> X
        if (matchBinOpOperandsCommutative(multiplyInstruction, mOne, X)) {
            return Assign.ofRegularAssign(multiplyInstruction.dst, X);
        }
        return multiplyInstruction;
    }

    private ThreeAddressCode simplifyQuadruple(Quadruple quadruple) {
        var aLong = InstructionSimplifyIrPass.symbolicallyEvaluate(String.format("%s %s %s", quadruple.fstOperand.value, quadruple.operator, quadruple.sndOperand.value));
        if (aLong.isPresent()) {
            return Assign.ofRegularAssign(quadruple.dst, new ConstantName(aLong.get(), BuiltinType.Int));
        }
        ThreeAddressCode newTac = null;
        switch (quadruple.operator) {
            case PLUS: {
                newTac = trySimplifyAddInstruction(quadruple);
                break;
            }
            case MULTIPLY: {
                newTac = trySimplifyMulInstruction(quadruple);
                break;
            }
            case EQ: {
                newTac = trySimplifyEqInstruction(quadruple);
                break;
            }
        }

        if (newTac != null)
            return newTac;
        return quadruple;
    }

    private ThreeAddressCode simplifyTriple(Triple triple) {
        return triple;
    }


    private void simplifyInstructions() {
        for (BasicBlock basicBlock : basicBlocks) {
            var newTacList = new ArrayList<ThreeAddressCode>();
            for (var tac : basicBlock.threeAddressCodeList) {
                ThreeAddressCode newTac;
                if (tac instanceof Quadruple) {
                    newTac = simplifyQuadruple((Quadruple) tac);
                } else if (tac instanceof Triple) {
                    newTac = simplifyTriple((Triple) tac);
                } else {
                    newTac = tac;
                }
                newTacList.add(newTac);
            }
            basicBlock.threeAddressCodeList.reset(newTacList);
        }
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.codes();
        simplifyInstructions();
        return oldCodes.equals(entryBlock.threeAddressCodeList.getCodes());
    }
}
