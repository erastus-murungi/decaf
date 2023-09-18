package decaf.ir.cfg;

import decaf.analysis.syntax.ast.*;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;
import decaf.shared.CompilationContext;
import decaf.shared.env.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class Cfg implements AstVisitor<Optional<CfgBlock>> {
    @Nullable CfgBlock continueJumpTarget;
    @NotNull CompilationContext context;
    @Nullable
    private CfgBlock currentBlock;

    @NotNull CfgBlock exitBlock;
    @Nullable
    private CfgBlock successor;
    private boolean malformedCfg = false;
    @Nullable
    private CfgBlock breakJumpTarget;

    private Cfg(@NotNull MethodDefinition methodDefinition, @NotNull CompilationContext context) {
        this.context = context;
        this.exitBlock = CfgBlock.empty();
        context.addExitCfgBlockFor(methodDefinition.getName(), exitBlock);
        this.currentBlock = CfgBlock.empty();
        context.addEntryCfgBlockFor(methodDefinition.getName(), currentBlock);
        methodDefinition.accept(this,
                                context.getScopeFor(methodDefinition)
                                       .orElseThrow(() -> new IllegalStateException(String.format(
                                               "No scope for method %s",
                                               methodDefinition.getName()
                                                                                                 )))
                               );

    }

    public static CfgBlock createGlobalCfgBlock(@NotNull Program program) {
        var globalCfgBlock = CfgBlock.empty();
        for (var fieldDeclaration: program.getFieldDeclarations())
            globalCfgBlock.addUserToEnd(fieldDeclaration);
        for (var methodDefinition: program.getMethodDefinitions())
            globalCfgBlock.addUserToEnd(methodDefinition);
        return globalCfgBlock;
    }

    public static void build(@NotNull CompilationContext compilationContext) {
        var globalBlock = createGlobalCfgBlock(compilationContext.getProgram());
        compilationContext.setGlobalEntryBlock(globalBlock);
        for (var methodDefinition: compilationContext.getProgram().getMethodDefinitions()) {
            var cfg = new Cfg(methodDefinition, compilationContext);
            compilationContext.setCfg(methodDefinition.getName(), cfg);
        }
    }

    @Override
    public Optional<CfgBlock> visit(IntLiteral intLiteral, Scope scope) {
        return Optional.empty();
    }

    // createBlock - Used to lazily create blocks that are connected
    // to the current (global) successor.
    @NotNull private CfgBlock createCfgBlockWithSuccessor() {
        return (successor != null) ? CfgBlock.withSuccessor(successor) : CfgBlock.empty();
    }

    // createBlock - Used to lazily create blocks
    private CfgBlock createCfgBlockNoSuccessor() {
        return CfgBlock.empty();
    }

    @NotNull private CfgBlock autoCreateBlock() {
        if (currentBlock == null) {
            currentBlock = createCfgBlockWithSuccessor();
        }
        return currentBlock;
    }

    @Override
    public Optional<CfgBlock> visit(BooleanLiteral booleanLiteral, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(FieldDeclaration fieldDeclaration, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(MethodDefinition methodDefinition, Scope scope) {
        checkState(currentBlock != null,
                   String.format("make sure an entry block has been created for method `%s`",
                                 methodDefinition.getName()
                                )
                  );
        methodDefinition.getFormalArguments().accept(this, scope);
        return methodDefinition.getBody().accept(this, scope);
    }

    @Override
    public Optional<CfgBlock> visit(ImportDeclaration importDeclaration, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(For forStatement, Scope scope) {
        return Optional.empty();
    }

    private void addSuccessor(CfgBlock currentBlock, CfgBlock successor) {
        if (currentBlock == null) {
            currentBlock = createCfgBlockWithSuccessor();
        }
        currentBlock.addSuccessor(successor);
        successor.addPredecessor(currentBlock);
    }

    @Override
    public Optional<CfgBlock> visit(@IsControlFlowStatement Break breakStatement, Scope scope) {
        if (malformedCfg) {
            return Optional.empty();
        }
        currentBlock = createCfgBlockNoSuccessor();
        currentBlock.addUserToEnd(breakStatement);
        assert currentBlock.getTerminator().map(t -> t == breakStatement).orElse(false);
        if (breakJumpTarget == null) {
            malformedCfg = true;
            return Optional.empty();
        } else {
            addSuccessor(currentBlock, breakJumpTarget);
            return Optional.ofNullable(currentBlock);
        }
    }

    @Override
    public Optional<CfgBlock> visit(@IsControlFlowStatement Continue continueStatement, Scope scope) {
        if (malformedCfg) {
            return Optional.empty();
        }
        currentBlock = createCfgBlockNoSuccessor();
        currentBlock.addUserToEnd(continueStatement);
        assert currentBlock.getTerminator().map(t -> t == continueStatement).orElse(false);
        if (continueJumpTarget == null) {
            malformedCfg = true;
            return Optional.empty();
        } else {
            addSuccessor(currentBlock, continueJumpTarget);
            return Optional.ofNullable(currentBlock);
        }
    }

    @Override
    public Optional<CfgBlock> visit(While whileStatement, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Program program, Scope scope) {
        throw new UnsupportedOperationException("CFGs are constructed on a a per method basis");
    }

    @Override
    public Optional<CfgBlock> visit(UnaryOpExpression unaryOpExpression, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(BinaryOpExpression binaryOpExpression, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Block block, Scope scope) {
        currentBlock = createCfgBlockWithSuccessor();
        for (var fieldDeclaration: block.getFieldDeclarations())
            currentBlock.addUserToFront(fieldDeclaration);
        for (var statement: block.getStatements()) {
            statement.accept(this, scope);
        }
        return Optional.ofNullable(currentBlock);
    }

    @Override
    public Optional<CfgBlock> visit(ParenthesizedExpression parenthesizedExpression, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(LocationArray locationArray, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(ExpressionParameter expressionParameter, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(If ifStatement, Scope scope) {
        if (currentBlock != null) {
            // The block we were processing is now finished.  Make it the successor
            // block.
            successor = currentBlock;
            if (malformedCfg) {
                return Optional.empty();
            }

            var elseBlock = Optional.of(currentBlock);
            final var savedElseBlock = elseBlock;
            // Process the false branch.
            if (ifStatement.getElseBlock().isPresent()) {
                // Create a new block for the false branch.
                currentBlock = null;
                // Process the false branch.
                elseBlock = ifStatement.getElseBlock().flatMap(block -> block.accept(this, scope));

                // If the false branch is not malformed, then the current block is
                // now the false branch.
                if (elseBlock.isEmpty()) {
                    elseBlock = savedElseBlock;
                } else if (currentBlock != null) {
                    // If the false branch is malformed, then the whole if statement is
                    // malformed.
                    if (malformedCfg) {
                        return Optional.empty();
                    }
                }
            }
            final var savedSuccessor = successor;
            // Create a new block for the true branch.
            currentBlock = null;
            // Process the true branch.
            var thenBlockOptional = ifStatement.getThenBlock().accept(this, scope);
            if (thenBlockOptional.isEmpty()) {
                var thenBlock = CfgBlock.withSuccessor(savedSuccessor);
                currentBlock = CfgBlock.withBothBranches(thenBlock, elseBlock.orElse(savedSuccessor));

                return ifStatement.getCondition().accept(this, scope);
                // TODO: short circuit processing here
            } else if (currentBlock != null) {
                if (malformedCfg) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Return returnStatement, Scope scope) {
        currentBlock = createCfgBlockNoSuccessor();
        currentBlock.addUserToEnd(returnStatement);
        currentBlock.addSuccessor(exitBlock);
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Array array, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(MethodCall methodCall, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(MethodCallStatement methodCallStatement, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(LocationAssignExpr locationAssignExpr, Scope scope) {
        checkNotNull(currentBlock, "current block cannot be null");
        currentBlock.addUserToFront(locationAssignExpr);
        return Optional.of(currentBlock);
    }

    @Override
    public Optional<CfgBlock> visit(AssignOpExpr assignOpExpr, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(FormalArgument formalArgument, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(RValue RValue, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(LocationVariable locationVariable, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Len len, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Increment increment, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Decrement decrement, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(CharLiteral charLiteral, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(StringLiteral stringLiteral, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(CompoundAssignOpExpr compoundAssignOpExpr, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Initialization initialization, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Assignment assignment, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(VoidExpression voidExpression, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(Type type, Scope scope) {
        return Optional.empty();
    }

    @Override
    public Optional<CfgBlock> visit(FormalArguments formalArguments, Scope scope) {
        checkNotNull(currentBlock, "current block cannot be null");
        currentBlock.addUserToFront(formalArguments);
        return Optional.of(currentBlock);
    }
}
