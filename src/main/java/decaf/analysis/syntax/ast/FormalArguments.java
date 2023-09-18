package decaf.analysis.syntax.ast;

import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.env.Scope;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class FormalArguments extends AST implements Iterable<FormalArgument> {
    private final List<FormalArgument> formalArguments;

    public FormalArguments(List<FormalArgument> formalArguments) {
        this.formalArguments = formalArguments;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Pair<String, AST>> getChildren() {
        return (List<Pair<String, AST>>) formalArguments.stream()
                                                        .map(formalArgument -> new Pair<>("formalArgument",
                                                                                          formalArgument
                                                        ));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(AstVisitor<T> astVisitor, Scope currentScope) {
        return astVisitor.visit(this, currentScope);
    }

    @Override
    public String getSourceCode() {
        return formalArguments.stream()
                              .map(formalArgument -> String.format("%s %s",
                                                                   formalArgument.getType().toString(),
                                                                   formalArgument.getName()
                                                                  ))
                              .collect(Collectors.joining(", "));
    }

    @NotNull
    @Override
    public Iterator<FormalArgument> iterator() {
        return formalArguments.iterator();
    }

    public boolean isEmpty() {
        return formalArguments.isEmpty();
    }

    public int size() {
        return formalArguments.size();
    }

    public FormalArgument get(int index) {
        return formalArguments.get(index);
    }

    public List<FormalArgument> toList() {
        return List.copyOf(formalArguments);
    }

    @Override
    public String toString() {
        return "FormalArguments{" +
               "formalArguments=" + formalArguments +
               '}';
    }
}
