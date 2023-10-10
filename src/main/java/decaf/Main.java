package decaf;

import decaf.ir.instructions.*;
import decaf.ir.types.IrType;
import decaf.ir.values.IrConstantInt;
import decaf.shared.TestRunner;

import java.util.Arrays;

class Main {
  public static void main(String[] args) {
//    TestRunner.testCfgBuilding("and-short-circuit", true, true);
    var allocaInst = AllocaInstruction.create(IrType.getIntType());
    var destination = allocaInst.getDestination();
    var loadInst = LoadInstruction.create(destination, allocaInst.getPointeeType());
    var storeInst = StoreInstruction.create(IrConstantInt.create(0), destination);
    var returnInstVoid = ReturnInstruction.create(IrConstantInt.create(10, 8));
    System.out.println(Arrays.toString(new Instruction[]{allocaInst, loadInst, storeInst, returnInstVoid}));
  }
}
