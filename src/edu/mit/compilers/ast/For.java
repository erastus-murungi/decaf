package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class For extends Statement {
    public final Name initId;
    public final Expression initExpression;

    final public Expression terminatingCondition;

    final public Location updatingLocation;
    final public AssignExpr updateAssignExpr;
    public final Block block;

    public For(TokenPosition tokenPosition, Name initId, Expression initExpression, Expression terminatingCondition, Location updatingLocation, AssignExpr updateAssignExpr, Block block) {
        super(tokenPosition);
        this.initId = initId;
        this.initExpression = initExpression;
        this.terminatingCondition = terminatingCondition;
        this.updatingLocation = updatingLocation;
        this.updateAssignExpr = updateAssignExpr;
        this.block = block;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("initId", initId), new Pair<>("initExpression", initExpression), new Pair<>("terminatingCondition", terminatingCondition), new Pair<>("location", updatingLocation), new Pair<>("updateAssignExpr", updateAssignExpr));
    }

    @Override
    public String toString() {
        return "For{" + "initId=" + initId + ", initExpression=" + initExpression + ", terminatingCondition=" + terminatingCondition + ", updatingLocation=" + updatingLocation + ", updateAssignExpr=" + updateAssignExpr + ", block=" + block + '}';
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }



}
