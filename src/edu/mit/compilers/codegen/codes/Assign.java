package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.dataflow.operand.AugmentedOperand;
import edu.mit.compilers.dataflow.operand.IncDecOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.utils.Utils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Assign extends Store implements Cloneable, HasOperand {
    public String assignmentOperator;
    public AbstractName operand;

    public Assign(AssignableName dst, String assignmentOperator, AbstractName operand, AST source, String comment) {
        super(dst, source, comment);
        this.assignmentOperator = assignmentOperator;
        this.operand = operand;
    }

    public static Assign ofRegularAssign(AssignableName dst, AbstractName operand) {
        return new Assign(dst, DecafScanner.ASSIGN, operand, null, "");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return List.of(store, operand);
    }

    @Override
    public String repr() {
        if (operand instanceof ConstantName)
            return String.format("%s%s: %s %s %s", DOUBLE_INDENT, store.repr(), store.builtinType.getSourceCode(), assignmentOperator, operand.repr());
        var load =  Utils.coloredPrint("load", Utils.ANSIColorConstants.ANSI_PURPLE_BOLD);
        return String.format("%s%s: %s %s %s %s", DOUBLE_INDENT, store.repr(), store.builtinType.getSourceCode(), assignmentOperator, load, operand.repr());
    }

    @Override
    public Instruction copy() {
        return new Assign(store, assignmentOperator, operand, source, getComment().orElse(null));
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s", DOUBLE_INDENT, store, assignmentOperator, operand);
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (operand instanceof ArrayName)
            return Optional.empty();
        switch (assignmentOperator) {
            case DecafScanner.ADD_ASSIGN:
            case DecafScanner.MINUS_ASSIGN:
            case DecafScanner.MULTIPLY_ASSIGN:
                return Optional.of(new AugmentedOperand(assignmentOperator, operand));
            case DecafScanner.INCREMENT:
            case DecafScanner.DECREMENT:
                return Optional.of(new IncDecOperand(assignmentOperator, (AssignableName) operand));
        }
        return Optional.of(new UnmodifiedOperand(operand));
    }

    public boolean contains(AbstractName name) {
        return store.equals(name) || operand.equals(name);
    }

    @Override
    public Assign clone() {
        Assign clone = (Assign) super.clone();
        // TODO: copy mutable state here, so the clone can't change the internals of the original
        clone.operand = operand;
        clone.assignmentOperator = assignmentOperator;
        clone.setComment(getComment().orElse(null));
        clone.store = store;
        clone.source = source;
        return clone;
    }

    @Override
    public Operand getOperand() {
        switch (assignmentOperator) {
            case DecafScanner.ADD_ASSIGN:
            case DecafScanner.MINUS_ASSIGN:
            case DecafScanner.MULTIPLY_ASSIGN:
                return new AugmentedOperand(assignmentOperator, operand);
            case DecafScanner.INCREMENT:
            case DecafScanner.DECREMENT:
                return new IncDecOperand(assignmentOperator, (AssignableName) operand);
        }
        return new UnmodifiedOperand(operand);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        if (Set.of("++", "--", "=").contains(assignmentOperator))
            return List.of(operand);
        return List.of(operand, getStore());
    }

    public boolean replace(AbstractName oldVariable, AbstractName replacer) {
        var replaced = false;
        if (operand.equals(oldVariable)) {
            operand = replacer;
            replaced = true;
        }
        return replaced;
    }

}
