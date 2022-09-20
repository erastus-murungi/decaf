package decaf.codegen;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import decaf.ast.Len;
import decaf.codegen.names.IrAssignable;
import decaf.codegen.names.IrMemoryAddress;
import decaf.ast.AST;
import decaf.ast.Assignment;
import decaf.ast.BinaryOpExpression;
import decaf.ast.Block;
import decaf.ast.BooleanLiteral;
import decaf.ast.CompoundAssignOpExpr;
import decaf.ast.ExpressionParameter;
import decaf.ast.FieldDeclaration;
import decaf.ast.Initialization;
import decaf.ast.IntLiteral;
import decaf.ast.Location;
import decaf.ast.LocationArray;
import decaf.ast.LocationAssignExpr;
import decaf.ast.LocationVariable;
import decaf.ast.MethodCall;
import decaf.ast.MethodCallParameter;
import decaf.ast.MethodCallStatement;
import decaf.ast.MethodDefinitionParameter;
import decaf.ast.Name;
import decaf.ast.ParenthesizedExpression;
import decaf.ast.Return;
import decaf.ast.StringLiteral;
import decaf.ast.Type;
import decaf.ast.UnaryOpExpression;
import decaf.codegen.codes.ArrayBoundsCheck;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.FunctionCallNoResult;
import decaf.codegen.codes.FunctionCallWithResult;
import decaf.codegen.codes.GetAddress;
import decaf.codegen.codes.ReturnInstruction;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.names.IrSsaRegister;
import decaf.codegen.names.IrIntegerConstant;
import decaf.codegen.names.IrStackArray;
import decaf.codegen.names.IrStringConstant;
import decaf.codegen.names.IrValue;
import decaf.descriptors.ArrayDescriptor;
import decaf.symboltable.SymbolTable;
import decaf.common.Operators;

class AstToInstructionListConverter implements CodegenAstVisitor<InstructionList> {
  private final SymbolTable symbolTable;
  private final HashMap<String, IrStringConstant> stringLiteralMapping;
  private final Map<String, IrValue> cachedAddresses = new HashMap<>();

  public AstToInstructionListConverter(
      SymbolTable symbolTable,
      HashMap<String, IrStringConstant> stringLiteralMapping,
      Set<IrValue> irGlobals
  ) {
    this.symbolTable = symbolTable;
    this.stringLiteralMapping = stringLiteralMapping;
    for (var global : irGlobals) {
      cachedAddresses.put(
          global.getLabel(),
          global
      );
    }
  }

  private IrValue newIrLocation(
      @NotNull String label,
      @NotNull Type type
  ) {
    if (cachedAddresses.containsKey(label)) return cachedAddresses.get(label)
                                                                  .copy();
    return new IrSsaRegister(
        label,
        type
    );
  }

  @Override
  public InstructionList visit(
      LocationArray locationArray,
      IrAssignable resultLocation
  ) {
    final var indexInstructionList = locationArray.expression.accept(
        this,
        resultLocation
    );
    final var locationArrayInstructionList = new InstructionList();
    locationArrayInstructionList.addAll(indexInstructionList);

    final var maybeArrayDescriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.getLabel());
    final var arrayDescriptor = (ArrayDescriptor) maybeArrayDescriptorFromValidScopes.orElseThrow(() -> new IllegalStateException(
        "expected to find array " + locationArray.name.getLabel() + " in scope"));
    var base = newIrLocation(
        locationArray.name.getLabel(),
        arrayDescriptor.type
    );
    final var getAddressInstruction = new GetAddress(
        base,
        indexInstructionList.getPlace(),
        generateAddressName(arrayDescriptor.type),
        arrayDescriptor.size,
        locationArray
    );
    locationArrayInstructionList.add(getAddressInstruction);

    if (!(indexInstructionList.getPlace() instanceof IrIntegerConstant)) {
      locationArrayInstructionList.add(new ArrayBoundsCheck(
          getAddressInstruction,
          IndexManager.getNextArrayBoundsCheckLabelIndex()
      ));
    }
    locationArrayInstructionList.setPlace(getAddressInstruction.getDestination());
    return locationArrayInstructionList;
  }

  private Stack<IrValue> lowerMethodCallParameters(
      @NotNull InstructionList instructionList,
      @NotNull List<MethodCallParameter> methodCallParameterList
  ) {
    var parameterIrRegisters = new Stack<IrValue>();
    for (MethodCallParameter methodCallParameter : methodCallParameterList) {
      var parameterInstructionList = methodCallParameter.accept(
          this,
          null
      );
      instructionList.addAll(parameterInstructionList);
      parameterIrRegisters.add(parameterInstructionList.getPlace());
    }
    return parameterIrRegisters;
  }

  private IrAssignable resolveStoreLocation(Type type) {
    return IrSsaRegister.gen(type);
  }

  private IrAssignable resolveStoreLocation(
      IrAssignable resultLocation,
      Type type
  ) {
    if (resultLocation == null) return resolveStoreLocation(type);
    return resultLocation.copy();
  }

  private IrMemoryAddress generateAddressName(
      Type type
  ) {
    return new IrMemoryAddress(
        IndexManager.genRegisterIndex(),
        Type.lower(type)
    );
  }

  @Override
  public InstructionList visit(
      MethodCall methodCall,
      IrAssignable resultLocation
  ) {
    final InstructionList instructionList = new InstructionList();
    var place = resolveStoreLocation(methodCall.getType());
    instructionList.add(new FunctionCallWithResult(
        methodCall,
        place,
        lowerMethodCallParameters(
            instructionList,
            methodCall.methodCallParameterList
        ),
        methodCall.getSourceCode()
    ));
    instructionList.setPlace(place.copy());
    return instructionList;
  }


  @Override
  public InstructionList visit(
      MethodCallStatement methodCallStatement,
      IrAssignable resultLocation
  ) {
    InstructionList instructionList = new InstructionList();
    instructionList.add(new FunctionCallNoResult(
        methodCallStatement.methodCall,
        lowerMethodCallParameters(
            instructionList,
            methodCallStatement.methodCall.methodCallParameterList
        ),
        methodCallStatement.getSourceCode()
    ));
    return instructionList;
  }

  @Override
  public InstructionList visit(
      LocationAssignExpr locationAssignExpr,
      IrAssignable resultLocation
  ) {
    final InstructionList lhs = locationAssignExpr.location.accept(
        this,
        resultLocation
    );
    IrSsaRegister locationVariable = (IrSsaRegister) lhs.getPlace();
    final InstructionList rhs = locationAssignExpr.assignExpr.accept(
        this,
        resultLocation
    );
    IrValue irValueVariable = rhs.getPlace();
    lhs.addAll(rhs);
    lhs.addAll(lowerAugmentedAssignment(
        locationVariable,
        irValueVariable,
        locationAssignExpr.getSourceCode(),
        locationAssignExpr
    ));
    return lhs;
  }

  @Override
  public InstructionList visit(
      MethodDefinitionParameter methodDefinitionParameter,
      IrAssignable resultLocation
  ) {
    return new InstructionList(new IrSsaRegister(
        methodDefinitionParameter.getName(),
        methodDefinitionParameter.getType()
    ));
  }

  @Override
  public InstructionList visit(
      Name name,
      IrAssignable resultLocation
  ) {
    Type type = symbolTable.getDescriptorFromValidScopes(name.getLabel())
                           .orElseThrow().type;
    return new InstructionList(newIrLocation(
        name.getLabel(),
        type
    ));
  }

  @Override
  public InstructionList visit(
      LocationVariable locationVariable,
      IrAssignable resultLocation
  ) {
    Type type = symbolTable.getDescriptorFromValidScopes(locationVariable.name.getLabel())
                           .orElseThrow().type;
    return new InstructionList(newIrLocation(
        locationVariable.name.getLabel(),
        type
    ));
  }

  @Override
  public InstructionList visit(Len len) {
    final ArrayDescriptor arrayDescriptor = (ArrayDescriptor) symbolTable.getDescriptorFromValidScopes(len.nameId.getLabel())
                                                                         .orElseThrow(() -> new IllegalStateException(
                                                                             len.nameId.getLabel() +
                                                                                 " should be present"));
    return new InstructionList(new IrIntegerConstant(
        arrayDescriptor.size,
        Type.Int
    ));
  }

  @Override
  public InstructionList visit(
      StringLiteral stringLiteral,
      IrAssignable resultLocation
  ) {
    return new InstructionList(stringLiteralMapping.get(stringLiteral.literal));
  }

  @Override
  public InstructionList visit(
      CompoundAssignOpExpr compoundAssignOpExpr,
      IrAssignable resultLocation
  ) {
    return compoundAssignOpExpr.expression.accept(
        this,
        resultLocation
    );
  }

  @Override
  public InstructionList visit(
      Initialization initialization,
      IrAssignable resultLocation
  ) {
    var initializationLocationInstructionList = initialization.initLocation.accept(
        this,
        resultLocation
    );
    var initializationExpressionInstructionList = initialization.initExpression.accept(
        this,
        resultLocation
    );
    var initializationInstructionList = new InstructionList(initializationLocationInstructionList.getPlace());
    initializationInstructionList.addAll(initializationLocationInstructionList);
    initializationInstructionList.addAll(initializationExpressionInstructionList);
    initializationInstructionList.add(new CopyInstruction(
        (IrSsaRegister) initializationLocationInstructionList.getPlace()
                                                             .copy(),
        initializationExpressionInstructionList.getPlace()
                                               .copy(),
        initialization,
        initialization.getSourceCode()
    ));
    return initializationInstructionList;
  }

  private InstructionList lowerAugmentedAssignment(
      @NotNull IrValue lhs,
      IrValue rhs,
      @NotNull String op,
      @NotNull AST assignment
  ) {
    var instructionList = new InstructionList(lhs);
    switch (op) {
      case Operators.ADD_ASSIGN, Operators.MINUS_ASSIGN, Operators.MULTIPLY_ASSIGN, Operators.DECREMENT, Operators.INCREMENT -> {
        final var constant = new IrIntegerConstant(
            1L,
            Type.Int
        );
        var dest = IrSsaRegister.gen(lhs.getType());
        var inst = switch (op) {
          case Operators.ADD_ASSIGN -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.PLUS,
              rhs.copy(),
              assignment.getSourceCode(),
              assignment
          );
          case Operators.MULTIPLY_ASSIGN -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.MULTIPLY,
              rhs.copy(),
              assignment.getSourceCode(),
              assignment
          );
          case Operators.MINUS_ASSIGN -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.MINUS,
              rhs.copy(),
              assignment.getSourceCode(),
              assignment
          );
          case Operators.DECREMENT -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.MINUS,
              constant.copy(),
              assignment.getSourceCode(),
              assignment
          );
          default -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.PLUS,
              constant.copy(),
              assignment.getSourceCode(),
              assignment
          );
        };
        instructionList.add(inst);
        instructionList.add(CopyInstruction.noAstConstructor(
            (IrSsaRegister) lhs.copy(),
            dest.copy()
        ));
      }
      default -> instructionList.add(new CopyInstruction(
          (IrAssignable) lhs.copy(),
          rhs.copy(),
          assignment,
          assignment.getSourceCode()
      ));
    }
    return instructionList;
  }

  @Override
  public InstructionList visit(
      @NotNull Assignment assignment,
      IrAssignable resultLocation
  ) {
    var assignmentInstructionList = new InstructionList();
    var storeLocationInstructionList = assignment.getLocation()
                                                 .accept(
                                                     this,
                                                     resultLocation
                                                 );
    InstructionList operandInstructionList;
    if (assignment.assignExpr.expression != null) {
      if (Operators.isCompoundOperator(assignment.getOperator())) {
        var type = assignment.assignExpr.expression.getType();
        operandInstructionList = assignment.assignExpr.expression.accept(
            this,
            IrSsaRegister.gen(type)
        );
      } else {
        operandInstructionList = assignment.assignExpr.expression.accept(
            this,
            (IrAssignable) storeLocationInstructionList.getPlace()
                                                       .copy()
        );
      }
    } else {
      operandInstructionList = new InstructionList();
    }
    assignmentInstructionList.addAll(storeLocationInstructionList);
    assignmentInstructionList.addAll(operandInstructionList);

    if (!(assignment.getOperator()
                    .equals(Operators.ASSIGN) && storeLocationInstructionList.getPlace()
                                                                             .equals(operandInstructionList.getPlace()))) {
      assignmentInstructionList.addAll(lowerAugmentedAssignment(
          storeLocationInstructionList.getPlace(),
          operandInstructionList.getPlace(),
          assignment.getOperator(),
          assignment
      ));
    }
    assignmentInstructionList.setPlace(storeLocationInstructionList.getPlace()
                                                                   .copy());
    return assignmentInstructionList;
  }

  @Override
  public InstructionList visit(
      BooleanLiteral booleanLiteral,
      IrAssignable resultLocation
  ) {
    return new InstructionList(IrIntegerConstant.fromBooleanLiteral(booleanLiteral));
  }

  @Override
  public InstructionList visit(
      IntLiteral intLiteral,
      IrAssignable resultLocation
  ) {
    return new InstructionList(IrIntegerConstant.fromIntLiteral(intLiteral));
  }

  @Override
  public InstructionList visit(
      FieldDeclaration fieldDeclaration,
      IrAssignable resultLocation
  ) {
    var instructionList = new InstructionList();
    for (var array : fieldDeclaration.arrays) {
      var length = array.getSize()
                        .convertToLong();
      var label = new IrStackArray(
          fieldDeclaration.getType(),
          array.getId()
               .getLabel(),
          length
      );
      cachedAddresses.put(
          array.getId()
               .getLabel(),
          label
      );
      for (long i = 0; i < length; i++) {
        var getAddressInstruction = new GetAddress(
            label,
            new IrIntegerConstant(
                i,
                Type.Int
            ),
            generateAddressName(fieldDeclaration.getType()),
            length,
            array
        );
        instructionList.add(getAddressInstruction);
        instructionList.add(new CopyInstruction(
            getAddressInstruction.getDestination()
                                 .copy(),
            IrIntegerConstant.zero(),
            array,
            String.format(
                "%s[%s] = 0",
                label,
                i
            )
        ));
      }
    }
    return instructionList;
  }

  @Override
  public InstructionList visit(
      UnaryOpExpression unaryOpExpression,
      IrAssignable resultLocation
  ) {
    final var operandInstructionList = unaryOpExpression.operand.accept(
        this,
        resultLocation
    );
    final var unaryOpExpressionInstructionList = new InstructionList();
    unaryOpExpressionInstructionList.addAll(operandInstructionList);
    var place = resolveStoreLocation(unaryOpExpression.getType());
    unaryOpExpressionInstructionList.add(new UnaryInstruction(
        place,
        unaryOpExpression.getUnaryOperator()
                         .getSourceCode(),
        operandInstructionList.getPlace()
                              .copy(),
        unaryOpExpression
    ));
    unaryOpExpressionInstructionList.setPlace(place.copy());
    return unaryOpExpressionInstructionList;
  }

  @Override
  public InstructionList visit(
      BinaryOpExpression binaryOpExpression,
      IrAssignable resultLocation
  ) {
    final var firstOperandInstructionList = binaryOpExpression.lhs.accept(
        this,
        null
    );
    final var secondOperandInstructionList = binaryOpExpression.rhs.accept(
        this,
        null
    );

    final var binaryOpExpressionInstructionList = new InstructionList();
    binaryOpExpressionInstructionList.addAll(firstOperandInstructionList);
    binaryOpExpressionInstructionList.addAll(secondOperandInstructionList);

    resultLocation = resolveStoreLocation(
        resultLocation,
        binaryOpExpression.getType()
    );
    binaryOpExpressionInstructionList.add(new BinaryInstruction(
        resultLocation,
        firstOperandInstructionList.getPlace()
                                   .copy(),
        binaryOpExpression.op.getSourceCode(),
        secondOperandInstructionList.getPlace()
                                    .copy(),
        binaryOpExpression.getSourceCode(),
        binaryOpExpression
    ));
    binaryOpExpressionInstructionList.setPlace(resultLocation.copy());
    return binaryOpExpressionInstructionList;
  }

  @Override
  public InstructionList visit(
      Block block,
      IrAssignable resultLocation
  ) {
    final InstructionList blockInstructionList = new InstructionList();
    block.fieldDeclarationList.forEach(fieldDeclaration -> blockInstructionList.addAll(fieldDeclaration.accept(
        this,
        null
    )));
    block.statementList.forEach(statement -> statement.accept(
        this,
        null
    ));
    return blockInstructionList;
  }

  @Override
  public InstructionList visit(
      ParenthesizedExpression parenthesizedExpression,
      IrAssignable resultLocation
  ) {
    return parenthesizedExpression.expression.accept(
        this,
        resultLocation
    );
  }

  @Override
  public InstructionList visit(
      ExpressionParameter expressionParameter,
      IrAssignable resultLocation
  ) {
    InstructionList expressionParameterInstructionList = new InstructionList();

    if (expressionParameter.expression instanceof LocationVariable) {
      // no need for temporaries
      expressionParameterInstructionList.setPlace(newIrLocation(
          ((Location) expressionParameter.expression).name.getLabel(),
          expressionParameter.expression.getType()
      ));
    } else if (expressionParameter.expression instanceof IntLiteral) {
      expressionParameterInstructionList.setPlace(new IrIntegerConstant(
          ((IntLiteral) expressionParameter.expression).convertToLong(),
          Type.Int
      ));
    } else {
      var temporaryVariable = IrSsaRegister.gen(expressionParameter.expression.getType());
      InstructionList expressionInstructionList = expressionParameter.expression.accept(
          this,
          resultLocation
      );
      expressionParameterInstructionList.addAll(expressionInstructionList);
      expressionParameterInstructionList.add(new CopyInstruction(
          temporaryVariable,
          expressionInstructionList.getPlace()
                                   .copy(),
          expressionParameter,
          temporaryVariable + " = " + expressionInstructionList.getPlace()
      ));
      expressionParameterInstructionList.setPlace(temporaryVariable.copy());
    }
    return expressionParameterInstructionList;
  }

  @Override
  public InstructionList visit(
      Return returnStatement,
      IrAssignable resultLocation
  ) {
    final InstructionList returnStatementInstructionList;
    if (returnStatement.retExpression != null) {
      InstructionList returnExpressionInstructionList = returnStatement.retExpression.accept(
          this,
          resultLocation
      );
      returnStatementInstructionList = new InstructionList();
      returnStatementInstructionList.addAll(returnExpressionInstructionList);
      returnStatementInstructionList.setPlace(returnExpressionInstructionList.getPlace());
      returnStatementInstructionList.add(new ReturnInstruction(
          returnStatement,
          returnStatementInstructionList.getPlace()
      ));
    } else {
      returnStatementInstructionList = new InstructionList();
      returnStatementInstructionList.add(new ReturnInstruction(returnStatement));
    }
    return returnStatementInstructionList;
  }
}
