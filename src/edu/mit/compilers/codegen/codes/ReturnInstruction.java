package edu.mit.compilers.codegen.codes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.utils.Utils;

public class ReturnInstruction extends HasOperand {
    private IrValue returnAddress;

    public ReturnInstruction(AST source) {
        super(source);
    }

    public ReturnInstruction(AST source, IrValue returnAddress) {
        super(source);
        this.returnAddress = returnAddress;
    }

    public Optional<IrValue> getReturnAddress() {
        return Optional.ofNullable(returnAddress);
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
        if (returnAddress == null)
            return Collections.emptyList();
        return List.of(returnAddress);
    }

    @Override
    public Instruction copy() {
        return new ReturnInstruction(source, returnAddress);
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(returnAddress);
    }

    @Override
    public List<IrValue> getOperandValues() {
        if (returnAddress == null)
            return Collections.emptyList();
        return List.of(returnAddress);
    }

    @Override
    public boolean replaceValue(IrValue oldName, IrValue newName) {
        if (oldName.equals(returnAddress)) {
            returnAddress = newName;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "return", getReturnAddress().isEmpty() ? " " : getReturnAddress().get().repr());
    }

    @Override
    public String syntaxHighlightedToString() {
        final var returnString = Utils.coloredPrint("return", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s", DOUBLE_INDENT, returnString, getReturnAddress().isEmpty() ? " " : getReturnAddress().get().repr());
    }
}
