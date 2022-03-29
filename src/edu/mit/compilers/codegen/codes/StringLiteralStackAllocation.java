package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.TemporaryNameGenerator;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Utils;

public class StringLiteralStackAllocation extends ThreeAddressCode {
    public String label;
    String stringConstant;
    String stringConstantEscaped;

    public StringLiteralStackAllocation(String stringConstant) {
        super(null);
        label = TemporaryNameGenerator.getNextStringLiteralIndex();
        this.stringConstant = stringConstant;
        this.stringConstantEscaped = Utils.translateEscapes(stringConstant.substring(1, stringConstant.length() - 1));
    }

    public int size() {
        return stringConstantEscaped.length();
    }

    public String toString() {
        return String.format("%s%s:\n%s%s%s", INDENT, label, DOUBLE_INDENT, stringConstant, DOUBLE_INDENT + " <<<< " + size() + " bytes");
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}
