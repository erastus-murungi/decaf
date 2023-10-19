package decaf;

import decaf.ir.BasicBlock;
import decaf.ir.IrInstructionPrettyPrinter;
import decaf.ir.IrInstructionValidator;
import decaf.ir.instructions.*;
import decaf.ir.types.IrFunctionType;
import decaf.ir.types.IrIntType;
import decaf.ir.values.IrLabel;
import decaf.ir.types.IrType;
import decaf.ir.values.IrConstantInt;
import decaf.ir.values.IrFunctionPointer;
import decaf.ir.values.IrRegister;
import decaf.shared.Pair;

import java.util.Arrays;

class Main {
    public static void main(String[] args) {
//    TestRunner.testCfgBuilding("and-short-circuit", true, true);
        var allocaInst = AllocaInstruction.create(IrIntType.getDefaultInt());
        var destination = allocaInst.getDestination();
        var loadInst = LoadInstruction.create(destination, allocaInst.getPointeeType());
        var storeInst = StoreInstruction.create(IrConstantInt.create(0), destination);
        var returnInst = ReturnInstruction.create(IrConstantInt.create(10, 8));
        var mulInst = BinaryInstruction.createMulGenDest(IrConstantInt.create(10, 8), IrConstantInt.create(10, 8));
        var copyInst = UnaryInstruction.createCopyGenDest(mulInst.getDestination());
        var branchInst = BranchInstruction.create(IrRegister.create(IrIntType.getBoolType()),
                                                  IrLabel.createNamed("if.then"),
                                                  IrLabel.createNamed("if.else")
                                                 );
        var zextInst = ZextInstruction.create(IrConstantInt.create(10, 1),
                                              IrRegister.create(IrIntType.getDefaultInt())
                                             );
        var jumpInst = UnconditionalBranchInstruction.create(IrLabel.createNamed("if.end"));
        var callInst = CallInstruction.createGenDest(IrFunctionPointer.create("add",
                                                                              IrFunctionType.create(IrIntType.getDefaultInt(),
                                                                                                    new IrType[]{IrIntType.getDefaultInt(), IrIntType.getDefaultInt()}
                                                                                                   )
                                                                             ));
        var phiInst = PhiInstruction.createFromPairsGenDest(Arrays.asList(PhiInstruction.createPhiSource(IrLabel.createNamed(
                                                                                  "if.then"), IrConstantInt.create(10, 8)),
                                                                          PhiInstruction.createPhiSource(IrLabel.createNamed(
                                                                                                                 "if.else"),
                                                                                                         IrRegister.create(
                                                                                                                 IrIntType.getDefaultInt())
                                                                                                        )
                                                                         ));
        var compInst = CompareInstruction.createEqGenDest(IrConstantInt.create(10, 8), IrConstantInt.create(10, 8));
        var instructions = new Instruction[]{allocaInst, loadInst, storeInst, mulInst, branchInst, phiInst, jumpInst, callInst, copyInst, returnInst};
        var basicBlock = BasicBlock.create(IrLabel.createNamed("entry"));
        basicBlock.addAll(Arrays.asList(instructions));
        System.out.println(basicBlock.prettyPrint());
        var validator = new IrInstructionValidator();
        validator.visit(mulInst, null);
        var printer = new IrInstructionPrettyPrinter();
        System.out.println(printer.visit(allocaInst, null));
        System.out.println(printer.visit(mulInst, null));
        System.out.println(printer.visit(jumpInst, null));
        System.out.println(printer.visit(branchInst, null));
        System.out.println(printer.visit(callInst, null));
        System.out.println(printer.visit(zextInst, null));
        System.out.println(printer.visit(compInst, null));
        System.out.println(printer.visit(loadInst, null));
        System.out.println(printer.visit(phiInst, null));
        System.out.println(printer.visit(returnInst, null));
        System.out.println(printer.visit(storeInst, null));
    }
}
