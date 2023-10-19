package decaf.ir;

import decaf.ir.instructions.*;
import decaf.ir.types.IrFunctionType;
import decaf.ir.types.IrIntType;
import decaf.ir.types.IrVoidType;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkState;

public class IrInstructionValidator implements IrInstructionVisitor<IrContext, Void> {
    @Override
    public @NotNull Void visit(@NotNull AllocaInstruction allocaInstruction, IrContext irContext) {
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull BinaryInstruction binaryInstruction, IrContext irContext) {
        checkState(binaryInstruction.getLhs().getType() == binaryInstruction.getRhs().getType(),
                   "binary lhs and rhs must have the same type"
                  );
        checkState(binaryInstruction.getLhs().getType().isFirstClassType(),
                   "binary lhs and rhs must first class types"
                  );
        checkState(binaryInstruction.getDestination().getType() == binaryInstruction.getLhs().getType(),
                   "binary destination and lhs must have the same type"
                  );
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull BranchInstruction branchInstruction, IrContext irContext) {
        checkState(branchInstruction.getCondition().getType() instanceof IrIntType &&
                   branchInstruction.getCondition().getType().getBitWidth() == 1,
                   "branch condition must be an int type"
                  );
        checkState(irContext.hasLabel(branchInstruction.getTrueTarget()), "branch true target must exist");
        checkState(irContext.hasLabel(branchInstruction.getFalseTarget()), "branch false target must exist");
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull CallInstruction callInstruction, IrContext irContext) {
        // check that the function type matches the argument types
        checkState(irContext.hasFunction(callInstruction.getFunctionPointer().getFunctionName()),
                   String.format("function %s must exist before being called; this can done by declaring/defining it first",
                                 callInstruction.getFunctionPointer().getFunctionName()
                                )
                  );
        checkState(callInstruction.getType() instanceof IrFunctionType, "call function must be a function type");
        var functionType = (IrFunctionType) callInstruction.getType();
        if (callInstruction.getDestination() == null) {
            checkState(functionType.getReturnType() == IrVoidType.get(),
                       "call destination and function return type must have the same type"
                      );
        } else {
            checkState(functionType.getReturnType() == callInstruction.getDestination().getType(),
                       "call destination and function return type must have the same type"
                      );
        }
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull ZextInstruction zextInstruction, IrContext irContext) {
        // The ‘zext’ instruction takes a value to cast, and a type to cast it to.
        // Both types must be of integer types, or vectors of the same number of integers.
        // The bit size of the value must be smaller than the bit size of the destination type, ty2.
        // The result is a value of type ty2.
        checkState(zextInstruction.getSrc().getType().isIntType(), "zext src must be an int type");
        checkState(zextInstruction.getDst().getType().isIntType(), "zext dst must be an int type");
        checkState(zextInstruction.getSrc().getType().getBitWidth() < zextInstruction.getDst().getType().getBitWidth(),
                   "zext src must be smaller than dst"
                  );
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull CompareInstruction compareInstruction, IrContext irContext) {
        checkState(compareInstruction.getLhs().getType() == compareInstruction.getRhs().getType(),
                   "compare lhs and rhs must have the same type"
                  );
        checkState(compareInstruction.getLhs().getType().isIntType() ||
                   compareInstruction.getLhs().getType().isPointerType(),
                   "compare lhs and rhs must be int or pointer types"
                  );
        checkState(compareInstruction.getDestination().getType() == IrIntType.getBoolType());
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull GetAddressInstruction getElementPtrInstruction, IrContext irContext) {
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull LoadInstruction loadInstruction, IrContext irContext) {
        checkState(loadInstruction.getDestination().getType() == loadInstruction.getType(),
                   "load memory address and destination must have the same type"
                  );
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull PhiInstruction phiInstruction, IrContext irContext) {
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull ReturnInstruction returnInstruction, IrContext irContext) {

        var currentFunction = irContext.getCurrentFunctionNonNull();
        var currentFunctionType = (IrFunctionType) currentFunction.getType();
        return (Void) returnInstruction.getReturnValue().map(irValue -> {
            checkState(currentFunctionType.getReturnType() == irValue.getType(),
                       "return value and function return type must have the same type"
                      );
            return null;
        }).orElseGet(() -> {
            checkState(currentFunctionType.getReturnType() == IrVoidType.get(),
                       "return value and function return type must have the same type"
                      );
            return null;
        });
    }

    @Override
    public @NotNull Void visit(@NotNull StoreInstruction storeInstruction, IrContext irContext) {
        // check that the type of the value to be stored is a first class type
        checkState(storeInstruction.getValue().getType().isFirstClassType(), "store value must be a first class type");
        checkState(storeInstruction.getValue().getType() == storeInstruction.getType(),
                   "store value and pointer must have the same type"
                  );
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull UnconditionalBranchInstruction unconditionalBranchInstruction,
                               IrContext irContext) {
        checkState(irContext.hasLabel(unconditionalBranchInstruction.getTarget()),
                   "unconditional branch target must exist"
                  );
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull UnaryInstruction unaryInstruction, IrContext irContext) {
        // check that the values are the same type
        checkState(unaryInstruction.getDestination().getType() == unaryInstruction.getOperand().getType(),
                   "unary destination and operand must have the same type"
                  );
        switch (unaryInstruction.getUnaryOpType()) {
            case NOT:
                checkState(unaryInstruction.getDestination().getType().isIntType(), "unary operand must be an int");
                break;
            case COPY:
                checkState(unaryInstruction.getDestination().getType().isFirstClassType(),
                           "unary operand must be a first class type"
                          );
            default:
                throw new IllegalStateException("unary op type must be `not` or `copy`");
        }
        return null;
    }

    @Override
    public @NotNull Void visit(@NotNull IrFunction irFunction, IrContext irContext) {
        return null;
    }
}
