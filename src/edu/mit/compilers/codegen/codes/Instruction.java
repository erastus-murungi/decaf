package edu.mit.compilers.codegen.codes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.codegen.names.IrMemoryAddress;
import edu.mit.compilers.codegen.names.IrRegister;

public abstract class Instruction {
    public static final String INDENT = "    ";
    public static final String DOUBLE_INDENT = INDENT + INDENT;
    public AST source;
    private String comment;

    public Instruction(AST source, String comment) {
        this.comment = comment;
        this.source = source;
    }

    public Instruction(AST source) {
        this(source, null);
    }

    public Instruction() {
        this(null, null);
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public abstract void accept(AsmWriter asmWriter);

    public abstract List<IrValue> getAllValues();

    public abstract String syntaxHighlightedToString();

    public abstract Instruction copy();

    public Collection<IrAssignableValue> getAllLValues() {
        return getAllValues().stream()
                .filter(value -> (value instanceof IrAssignableValue))
                .map(value -> (IrAssignableValue) value)
                .collect(Collectors.toList());
    }

    /**
     * This includes registers to store array base addresses, array indices, local variables
     */
    public Collection<IrAssignableValue> getAllRegisterAllocatableValues() {
        var lValues = getAllLValues();
        var memoryAddresses = lValues.stream()
                                     .filter(lValue -> lValue instanceof IrMemoryAddress)
                                     .map(lValue -> (IrMemoryAddress) lValue)
                                     .map(IrMemoryAddress::getBaseAddress)
                                     .toList();
        if (!memoryAddresses.isEmpty()) {
            var lValuesArray = new ArrayList<>(lValues);
            lValuesArray.addAll(lValues);
            return lValuesArray;
        }
        return lValues;
    }

    public Collection<IrRegister> getAllVirtualRegisters() {
        return getAllValues().stream()
                .filter(value -> (value instanceof IrRegister))
                .map(value -> (IrRegister) value)
                .collect(Collectors.toList());

    }

    public String noCommentsSyntaxHighlighted() {
        if (getComment().isPresent()) {
            String comment = getComment().get();
            setComment(null);
            var res = syntaxHighlightedToString();
            setComment(comment);
            return res;
        }
        return syntaxHighlightedToString();
    }

    public String noCommentsToString() {
        if (getComment().isPresent()) {
            String comment = getComment().get();
            setComment(null);
            var res = toString();
            setComment(comment);
            return res;
        }
        return toString();
    }
}
