package edu.mit.compilers.asm.operands;

import org.jetbrains.annotations.Nullable;

import edu.mit.compilers.codegen.names.IrValue;

public abstract class X86MappedValue extends X86Value{
    public X86MappedValue(@Nullable IrValue irValue) {
        super(irValue);
    }
}
