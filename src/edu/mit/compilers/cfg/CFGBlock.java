package edu.mit.compilers.cfg;

import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class CFGBlock {
    public ArrayList<CFGBlock> parents;
    public ArrayList<CFGLine> lines;
    private ThreeAddressCodeList threeAddressCodeList;

    // to be set later by the ThreeAddressListVisitor
    public void setThreeAddressCodeList(ThreeAddressCodeList threeAddressCodeList) {
        this.threeAddressCodeList = threeAddressCodeList;
    }

    public Optional<ThreeAddressCodeList> getThreeAddressCodeList() {
        return Optional.of(threeAddressCodeList);
    }

    public CFGBlock() {
        parents = new ArrayList<>();
        lines = new ArrayList<>();
    }

    public<T> T accept(CFGVisitor<T> visitor, SymbolTable symbolTable) {
        if (this instanceof CFGConditional)
            return visitor.visit((CFGConditional)this, symbolTable);
        else if (this instanceof NOP)
            return visitor.visit((NOP)this, symbolTable);
        else return visitor.visit((CFGNonConditional) this, symbolTable);
    };

    public String getLabel() {
        List<String> list = new ArrayList<>();
        for (CFGLine cfgLine : lines) {
            String sourceCode = cfgLine.ast.getSourceCode();
            list.add(sourceCode);
        }
        String ret =  String.join("\n", list);
        if (ret.isBlank()) {
            return "∅";
        }
        return ret;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
