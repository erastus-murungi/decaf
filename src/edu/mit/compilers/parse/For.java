package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class For extends Statement {
    final Name initId;
    final Expr initExpr;

    final Expr terminatingCondition;

    final Location updatingLocation;
    final AssignExpr updateAssignExpr;
    final Block block;

    public For(TokenPosition tokenPosition, Name initId, Expr initExpr, Expr terminatingCondition, Location updatingLocation, AssignExpr updateAssignExpr, Block block) {
        super(tokenPosition);
        this.initId = initId;
        this.initExpr = initExpr;
        this.terminatingCondition = terminatingCondition;
        this.updatingLocation = updatingLocation;
        this.updateAssignExpr = updateAssignExpr;
        this.block = block;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("initId", initId), new Pair<>("initExpr", initExpr), new Pair<>("terminatingCondition", terminatingCondition), new Pair<>("location", updatingLocation), new Pair<>("updateAssignExpr", updateAssignExpr));
    }

    @Override
    public String toString() {
        return "For{" + "initId=" + initId + ", initExpr=" + initExpr + ", terminatingCondition=" + terminatingCondition + ", updatingLocation=" + updatingLocation + ", updateAssignExpr=" + updateAssignExpr + ", block=" + block + '}';
    }

    @Override
    public boolean isTerminal() {
        return false;
    }



}
