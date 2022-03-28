package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodesVisitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class ThreeAddressListFillerVisitor implements CFGVisitor<ThreeAddressCodeList> {
    ThreeAddressCodesVisitor visitor;

    public ThreeAddressListFillerVisitor(ThreeAddressCodesVisitor visitor) {
        this.visitor = visitor;
    }

    @Override
    public ThreeAddressCodeList visit(CFGNonConditional cfgNonConditional, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(CFGConditional cfgConditional, SymbolTable symbolTable) {
        Expression condition = (Expression) (cfgConditional.condition).ast;

        if (condition instanceof BinaryOpExpression)
            return visitor.visit((BinaryOpExpression) condition, symbolTable);
        else if (condition instanceof UnaryOpExpression)
            return visitor.visit((UnaryOpExpression) condition, symbolTable);
        else if (condition instanceof MethodCall)
            return visitor.visit((MethodCall) condition, symbolTable);
        else
            throw new IllegalStateException("an expression of type + " + condition.toString() + " is not allowed");
    }

    @Override
    public ThreeAddressCodeList visit(NOP nop, SymbolTable symbolTable) {
        throw new IllegalStateException("There should be no NOPs at this point, call NopVisitor to deal with it");
    }
}
