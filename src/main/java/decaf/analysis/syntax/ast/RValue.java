package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;

import java.util.Collections;
import java.util.List;


public class RValue extends AST {
    private String label;

    public RValue(TokenPosition tokenPosition, String label) {
        super(tokenPosition);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "Name{" + "label='" + label + '\'' + '}';
    }

    @Override
    public String getSourceCode() {
        return label;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public <ReturnType, InputType> ReturnType accept(AstVisitor<ReturnType, InputType> astVisitor, InputType input) {
        return astVisitor.visit(this, input);
    }
}
