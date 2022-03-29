package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class Update extends AST {
    final public Location updateLocation;
    final public AssignExpr updateAssignExpr;

    public Update(Location updatingLocation, AssignExpr updateAssignExpr) {
        this.updateLocation = updatingLocation;
        this.updateAssignExpr = updateAssignExpr;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("updateLocation", updateLocation), new Pair<>("updateAssignExpr", updateAssignExpr));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable) {
        return visitor.visit(this, currentSymbolTable);
    }

    @Override
    public String getSourceCode() {
        return String.format("%s %s", updateLocation.getSourceCode(), updateAssignExpr.getSourceCode());
    }

    @Override
    public String toString() {
        return "Update{" +
                "updateLocation=" + updateLocation +
                ", updateAssignExpr=" + updateAssignExpr +
                '}';
    }
}
