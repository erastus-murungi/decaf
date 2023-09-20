package decaf.ir.cfg;

import decaf.analysis.syntax.ast.*;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;
import decaf.shared.CompilationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Cfg implements AstVisitor<@NotNull CfgBlock, @NotNull CfgBlock> {
    @NotNull
    private final CompilationContext context;
    @NotNull
    private final CfgBlock entryBlock;
    @Nullable
    private CfgBlock breakJumpTarget;
    @Nullable
    private CfgBlock continueJumpTarget;
    @Nullable
    private CfgBlock exitBlock;

    @NotNull private CfgBlock successorBlock;

    @NotNull
    private final String methodName;

    private Cfg(@NotNull MethodDefinition methodDefinition, @NotNull CompilationContext context) {
        this.context = context;
        entryBlock = CfgBlock.empty();
        methodName = methodDefinition.getName();
        successorBlock = CfgBlock.empty();
        context.addEntryCfgBlockFor(methodDefinition.getName(), entryBlock);
        methodDefinition.accept(this, entryBlock);
    }

    public static @NotNull CfgBlock createGlobalCfgBlock(@NotNull Program program) {
        var globalCfgBlock = CfgBlock.empty();
        for (var fieldDeclaration : program.getFieldDeclarations()) {
            globalCfgBlock.addUserToEnd(fieldDeclaration);
        }
        for (var methodDefinition : program.getMethodDefinitions()) {
            globalCfgBlock.addUserToEnd(methodDefinition);
        }
        return globalCfgBlock;
    }

    public static void build(@NotNull CompilationContext compilationContext) {
        var globalBlock = createGlobalCfgBlock(compilationContext.getProgram());
        compilationContext.setGlobalEntryBlock(globalBlock);
        for (var methodDefinition : compilationContext.getProgram().getMethodDefinitions()) {
            var cfg = new Cfg(methodDefinition, compilationContext);
            compilationContext.setCfg(methodDefinition.getName(), cfg);
            globalBlock.addSuccessor(cfg.getEntryBlock());
        }
    }

    public @NotNull CfgBlock getEntryBlock() {
        return entryBlock;
    }

    private @NotNull CfgBlock getSuccessorBlock(@NotNull CfgBlock currentBlock) {
        if (currentBlock.isEmpty()) {
            successorBlock = currentBlock;
            return currentBlock;
        } else {
            assert successorBlock.isEmpty();
            var toReturn = successorBlock;
            successorBlock = CfgBlock.empty();
            return toReturn;
        }
    }

    @Override
    public @NotNull CfgBlock visit(IntLiteral intLiteral, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(BooleanLiteral booleanLiteral, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(FieldDeclaration fieldDeclaration, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(MethodDefinition methodDefinition, CfgBlock currentBlock) {
        methodDefinition.getFormalArguments().accept(this, currentBlock);
        return methodDefinition.getBody().accept(this, currentBlock);
    }

    @Override
    public @NotNull CfgBlock visit(ImportDeclaration importDeclaration, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(For forStatement, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(@IsControlFlowStatement @NotNull Break breakStatement, CfgBlock currentBlock) {
        currentBlock.addUserToEnd(breakStatement);
        assert currentBlock.getTerminator().map(t -> t == breakStatement).orElse(false);
        if (breakJumpTarget == null) {
            throw new MalformedSourceLevelCfg("break statement outside of a loop");
        } else {
            var newBlock = CfgBlock.empty();
            currentBlock.addSuccessor(newBlock);
            return newBlock;
        }
    }

    @Override
    public @NotNull CfgBlock visit(@IsControlFlowStatement @NotNull Continue continueStatement, CfgBlock currentBlock) {
        var newBlock = CfgBlock.empty();
        newBlock.addUserToEnd(continueStatement);
        assert newBlock.getTerminator().map(t -> t == continueStatement).orElse(false);
        if (continueJumpTarget == null) {
            throw new MalformedSourceLevelCfg("continue statement outside of a loop");
        } else {
            continueJumpTarget.addSuccessor(newBlock);
            return newBlock;
        }
    }

    @Override
    public @NotNull CfgBlock visit(While whileStatement, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(Program program, CfgBlock currentBlock) {
        throw new UnsupportedOperationException("CFGs are constructed on a a per method basis");
    }

    @Override
    public @NotNull CfgBlock visit(UnaryOpExpression unaryOpExpression, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(BinaryOpExpression binaryOpExpression, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(Block block, CfgBlock currentBlock) {
        currentBlock.addUsers(block.getFieldDeclarations());
        var lastBlock = currentBlock;
        for (var statement : block.getStatements()) {
            lastBlock = statement.accept(this, currentBlock);
        }
        return lastBlock;
    }

    @Override
    public @NotNull CfgBlock visit(ParenthesizedExpression parenthesizedExpression, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(LocationArray locationArray, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(@NotNull ExpressionParameter expressionParameter, @NotNull CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(@NotNull If ifStatement, @NotNull CfgBlock currentBlock) {
        // The block we were processing is now finished.
        // Make it the successor block.
        // Process the false branch.

        var nextBlock = CfgBlock.empty();
        if (ifStatement.getElseBlock().isPresent()) {
            var elseBlock = ifStatement.getElseBlock().get();
            // Create a new block for the false branch.
            // Process the false branch.
            var elseBlockEntry = CfgBlock.empty();
            var elseBlockExit = elseBlock.accept(this, elseBlockEntry);
            var thenBlockEntry = CfgBlock.empty();
            var thenBlockExit = ifStatement.getThenBlock().accept(this, thenBlockEntry);
            currentBlock.addSuccessor(thenBlockEntry);
            currentBlock.addSuccessor(elseBlockEntry);
            // TOD0: add short-circuiting for && and ||
            currentBlock.addUserToEnd(ifStatement.getCondition());
            thenBlockExit.addSuccessor(nextBlock);
            elseBlockExit.addSuccessor(nextBlock);
            return nextBlock;
        } else {
            var thenBlockEntry = CfgBlock.empty();
            var thenBlockExit = ifStatement.getThenBlock().accept(this, thenBlockEntry);
            currentBlock.addSuccessor(thenBlockEntry);
            thenBlockExit.addSuccessor(nextBlock);
            // TOD0: add short-circuiting for && and ||
            currentBlock.addUserToEnd(ifStatement.getCondition());
            return nextBlock;
        }
    }

    @Override
    public @NotNull CfgBlock visit(@NotNull Return returnStatement, @NotNull CfgBlock currentBlock) {
        // create a new exit block only if we don't already have one
        if (exitBlock == null) {
            if (currentBlock.isEmpty()) {
                exitBlock = currentBlock;
            } else {
                exitBlock = CfgBlock.empty();
            }
            context.addExitCfgBlockFor(methodName, exitBlock);
        }
        currentBlock.addSuccessor(exitBlock);
        currentBlock.addUserToEnd(returnStatement);
        return exitBlock;
    }

    @Override
    public @NotNull CfgBlock visit(Array array, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(MethodCall methodCall, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(MethodCallStatement methodCallStatement, CfgBlock currentBlock) {
        currentBlock.addUserToEnd(methodCallStatement);
        return currentBlock;
    }

    @Override
    public @NotNull CfgBlock visit(LocationAssignExpr locationAssignExpr, CfgBlock currentBlock) {
        currentBlock.addUserToEnd(locationAssignExpr);
        return currentBlock;
    }

    @Override
    public @NotNull CfgBlock visit(AssignOpExpr assignOpExpr, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(FormalArgument formalArgument, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(RValue RValue, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(LocationVariable locationVariable, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(Len len, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(Increment increment, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(Decrement decrement, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(CharLiteral charLiteral, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(StringLiteral stringLiteral, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(CompoundAssignOpExpr compoundAssignOpExpr, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(Initialization initialization, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(Assignment assignment, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(VoidExpression voidExpression, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(Type type, CfgBlock currentBlock) {
        return null;
    }

    @Override
    public @NotNull CfgBlock visit(FormalArguments formalArguments, CfgBlock currentBlock) {
        currentBlock.addUserToFront(formalArguments);
        return currentBlock;
    }
}
