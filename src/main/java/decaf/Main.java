package decaf;

import decaf.ir.BasicBlock;
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
        var destination = allocaInst.getAddress();
        var loadInst = LoadInstruction.create(destination, allocaInst.getPointeeType());
        var storeInst = StoreInstruction.create(IrConstantInt.create(0), destination);
        var returnInst = ReturnInstruction.create(IrConstantInt.create(10, 8));
        var mulInst = BinaryInstruction.createMulGenDest(IrConstantInt.create(10, 8), IrConstantInt.create(10, 8));
        var copyInst = UnaryInstruction.createCopyGenDest(mulInst.getDestination());
        var branchInst = BranchInstruction.create(IrRegister.create(IrIntType.getBoolType()),
                                                  IrLabel.createNamed("if.then"),
                                                  IrLabel.createNamed("if.else")
                                                 );
        var jumpInst = UnconditionalBranchInstruction.create(IrLabel.createNamed("if.end"));
        var callInst = CallInstruction.createGenDest(IrFunctionPointer.create("add",
                                                                              IrFunctionType.create(IrIntType.getDefaultInt(),
                                                                                                    new IrType[]{IrIntType.getDefaultInt(), IrIntType.getDefaultInt()}
                                                                                                   )
                                                                             ));
        var phiInst = PhiInstruction.createFromPairsGenDest(Arrays.asList(new Pair<>(IrLabel.createNamed("if.then"),
                                                                                     IrConstantInt.create(10, 8)
        ), new Pair<>(IrLabel.createNamed("if.else"), IrRegister.create(IrIntType.getDefaultInt()))));
        var instructions = new Instruction[]{allocaInst, loadInst, storeInst, mulInst, branchInst, phiInst, jumpInst, callInst, copyInst, returnInst};
        var basicBlock = BasicBlock.create(IrLabel.createNamed("entry"));
        basicBlock.addAll(Arrays.asList(instructions));
        System.out.println(basicBlock.prettyPrint());
    }
}
