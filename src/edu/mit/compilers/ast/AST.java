package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.List;
import java.util.Optional;

public abstract class AST {
    private ThreeAddressCodeList threeAddressCodeList;

    public void setThreeAddressCodeList(ThreeAddressCodeList threeAddressCodeList) {
        this.threeAddressCodeList = threeAddressCodeList;
    }

    public Optional<ThreeAddressCodeList> getThreeAddressCodeList() {
        return Optional.of(threeAddressCodeList);
    }

    public abstract List<Pair<String, AST>> getChildren();

    public abstract boolean isTerminal();

    public abstract <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable);

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public abstract String getSourceCode();
}
