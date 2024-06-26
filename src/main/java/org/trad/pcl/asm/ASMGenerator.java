package org.trad.pcl.asm;

import org.trad.pcl.Exceptions.Semantic.UndefinedVariableException;
import org.trad.pcl.Helpers.TypeEnum;
import org.trad.pcl.ast.ParameterNode;
import org.trad.pcl.ast.ProgramNode;
import org.trad.pcl.ast.declaration.*;
import org.trad.pcl.ast.expression.*;
import org.trad.pcl.ast.statement.*;
import org.trad.pcl.ast.type.TypeNode;
import org.trad.pcl.semantic.ASTNodeVisitor;
import org.trad.pcl.semantic.StackTDS;
import org.trad.pcl.semantic.SymbolTable;
import org.trad.pcl.semantic.symbol.Record;
import org.trad.pcl.semantic.symbol.*;

import java.util.ArrayList;
import java.util.List;

import static org.trad.pcl.asm.ASMUtils.*;

public final class ASMGenerator implements ASTNodeVisitor {

    public final static StackTDS scopeStack = new StackTDS();
    private final List<SymbolTable> symbolTables;
    private final StringBuilder output;
    private int currentTableIndex;

    public ASMGenerator(List<SymbolTable> symbolTables) {
        this.symbolTables = symbolTables;
        output = new StringBuilder();
        scopeStack.push(symbolTables.get(0));
        currentTableIndex = 1;

    }

    public String getOutput() {
        return this.output.toString();
    }

    @Override
    public void visit(FunctionDeclarationNode node) throws Exception {
        enterScope();
        Function symbol = (Function) scopeStack.findSymbolInScopes(node.getIdentifier());
        this.output.append(symbol.getIdentifier()).append("\n").append("""
                \t STMFD   R13!, {R10, LR} ; Save caller's frame pointer and return ASM address
                \t MOV     R10, R9 ; Set up new static link
                \t SUB     R13, R13, #4
                \t STR     R11, [R13]
                \t MOV     R11, R13 ; Set up new frame pointer
                """);

        Type type = (Type) scopeStack.findSymbolInScopes(symbol.getReturnType());
        int counter = symbol.getParametersSize() + 2 * 4 + type.getSize();
        for (ParameterNode param : node.getParameters()) {
            Type paramType = (Type) scopeStack.findSymbolInScopes(param.getType().getIdentifier());
            if (paramType instanceof Record record) {
                this.output.append("""
                        \t ADD     R1, R11, #%s ; Load field in R0
                        \t SUB     R2, R13, #4 ; Store argument for %s in stack-frame
                        """.formatted(counter, symbol.getIdentifier()));
                this.output.append(saveRecordInStack(record, 0));
                this.output.append("""
                        \t SUB     R13, R13, #%s ;
                        """.formatted(record.getSize()));
            } else {
                this.output.append("""
                        \t LDR     R0, [R11, #%s] ; Load parameter %s in R0
                        \t STMFD   R13!, {R0} ; Store parameter %s in stack-frame
                        """.formatted(counter, param.getIdentifier(), param.getIdentifier()));

            }
            counter -= paramType.getSize();


        }
        node.getBody().accept(this);
        scopeStack.exitScope();
    }

    @Override
    public void visit(ProcedureDeclarationNode node) throws Exception {
        enterScope();
        Procedure symbol = (Procedure) scopeStack.findSymbolInScopes(node.getIdentifier());

        this.output.append(symbol.getIdentifier()).append("\n").append("""
                \t STMFD   R13!, {R10, LR} ; Save caller's frame pointer and return ASM address
                \t MOV     R10, R9 ; Set up new static link
                \t SUB     R13, R13, #4
                \t STR     R11, [R13]
                \t MOV     R11, R13 ; Set up new frame pointer
                """);
        int counter = symbol.getParametersSize() + 2 * 4;
        for (ParameterNode param : node.getParameters()) {
            Type paramType = (Type) scopeStack.findSymbolInScopes(param.getType().getIdentifier());
            if (paramType instanceof Record record) {
                this.output.append("""
                        \t ADD     R1, R11, #%s ; Load field in R0
                        \t SUB     R2, R13, #4 ; Store argument for %s in stack-frame
                        """.formatted(counter, symbol.getIdentifier()));
                this.output.append(saveRecordInStack(record, 0));
                this.output.append("""
                        \t SUB     R13, R13, #%s ;
                        """.formatted(record.getSize()));
            } else {
                this.output.append("""
                        \t LDR     R0, [R11, #%s] ; Load parameter %s in R0
                        \t STMFD   R13!, {R0} ; Store parameter %s in stack-frame
                        """.formatted(counter, param.getIdentifier(), param.getIdentifier()));

            }
            counter -= paramType.getSize();


        }
        node.getBody().accept(this);
        if(!node.checkHasReturn()){
            this.output.append("""
                \t MOV     R13, R11 ; Restore frame pointer
                \t LDR     R11, [R13] ; Restore caller's frame pointer
                \t ADD     R13, R13, #4 ;
                \t LDMFD   R13!, {R10, PC} ; Restore caller's frame pointer and return ASM address
                """);
        }

        scopeStack.exitScope();
    }

    @Override
    public void visit(TypeDeclarationNode node) throws Exception {

    }

    @Override
    public void visit(VariableDeclarationNode node) throws Exception {
        String type = node.getType().getIdentifier();
        Type typeSymbol = (Type) scopeStack.findSymbolInScopes(type);
        String formattedCode = String.format("\t SUB     R13, R13, #%s ; Save space for %s in stack-frame", typeSymbol.getSize(), node.getIdentifier());
        this.output.append(formattedCode).append("\n");

        if (node.getAssignment() != null) {
            node.getAssignment().accept(this);
        }
    }


    @Override
    public void visit(AssignmentStatementNode node) throws Exception {
        node.getExpression().accept(this); // store result in R0
        if (((Type) scopeStack.findSymbolInScopes(node.getExpression().getType(scopeStack))) instanceof Record record) {
            this.output.append("""
                    \t MOV     R1, R9 ; Load record address in R1
                    """);
            this.output.append(findVariableAddress(node.getVariableReference().getIdentifier(), node.getVariableReference().getNextExpression())); // store in address in R9
            this.output.append("""
                    \t MOV     R2, R9 ; Load variable address in R2
                    """);
            this.output.append(saveRecordInStack(record, 0));
        } else {
            this.output.append(findVariableAddress(node.getVariableReference().getIdentifier(), node.getVariableReference().getNextExpression())); // store in address in R9
            this.output.append("""
                    \t STR     R0, [R9] ; Assign right expression (assuming result is in R0) to left variable %s
                    """.formatted(node.getVariableReference().getIdentifier()));
        }
    }

    @Override
    public void visit(BlockNode node) {
        List<DeclarationNode> tempDeclarations = new ArrayList<>();
        node.getDeclarations().forEach(declaration -> {
            try {
                if (declaration instanceof ProcedureDeclarationNode || declaration instanceof FunctionDeclarationNode) {
                    tempDeclarations.add(declaration);
                } else {
                    declaration.accept(this);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        node.getStatements().forEach(statement -> {
            try {
                statement.accept(this);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        tempDeclarations.forEach(declaration -> {
            try {
                declaration.accept(this);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @Override
    public void visit(CallNode node) throws Exception {
        Procedure symbol = (Procedure) scopeStack.findSymbolInScopes(node.getIdentifier());

        if (symbol.getIdentifier().equals("put")) {
            node.getArguments().forEach(arg -> {
                try {
                    arg.accept(this);
                    this.output.append("""
                            \t SUB SP, SP, #4   ; réservez 4 octets pour le 0
                            \t MOV R1, #0
                            \t STR R1, [SP]
                            \t SUB SP, SP, #4   ; réservez 4 octets pour la valeur (ou plus)
                            \t STR R0, [SP]     ; stockez la valeur
                            \t MOV R0, SP       ; adresse de la valeur (ici SP, mais peut être n'importe quelle adresse)
                            \t BL print
                            \t ADD SP, SP, #8   ; libérez la pile
                            """);

                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            return;
        }


        if (symbol.getIdentifier().equals("new_line")) {
            try {
                this.output.append("""
                        \t ; new_line
                        \t SUB SP, SP, #4   ; réservez 4 octets pour le 0
                        \t MOV R1, #0
                        \t STR R1, [SP]
                        \t SUB SP, SP, #4   ; réservez 4 octets pour la valeur (ou plus)
                        \t MOV R0, #32
                        \t STR R0, [SP]     ; stockez la valeur
                        \t MOV R0, SP       ; adresse de la valeur (ici SP, mais peut être n'importe quelle adresse)
                        \t BL println
                        \t ADD SP, SP, #8   ; libérez la pile
                        """);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return;
        }

        if (symbol.getIdentifier().equals("putInt")) {
            node.getArguments().forEach(arg -> {
                try {
                    arg.accept(this);
                    this.output.append("""
                            \t LDR R3, =STR_OUT       ; on charge l'adresse de la valeur
                            \t BL to_ascii          ; on convertit l'entier en chaîne de caractères
                            \t LDR R0, =STR_OUT       ; on charge l'adresse de la chaîne de caractères
                            \t BL print           ; on affiche la chaîne de caractères
                            """);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            return;
        }

        node.getArguments().forEach(arg -> {
            try {
                arg.accept(this);
                if ((Type) scopeStack.findSymbolInScopes(arg.getType(scopeStack)) instanceof Record record) {
                    this.output.append("""
                            \t MOV     R1, R9 ; Load field in R0
                            \t SUB     R2, R13, #4 ; Store argument for %s in stack-frame
                            """.formatted(symbol.getIdentifier()));
                    this.output.append(saveRecordInStack(record, 0));
                    this.output.append("""
                            \t SUB     R13, R13, #%s ; Save space for argument
                            """.formatted(record.getSize()));
                } else {
                    this.output.append("""
                            \t STMFD   R13!, {R0} ; Save argument
                            """);
                }

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        if (symbol instanceof Function function) {
            Type type = (Type) scopeStack.findSymbolInScopes(function.getReturnType());
            this.output.append("""
                    \t SUB     R13, R13, #%s ; Save space for return value
                    """.formatted(type.getSize()));

            this.output.append(findAddress(node.getIdentifier()));

            this.output.append("""
                    \t BL      %s ; Branch link to %s (it will save the return address in LR)
                    """.formatted(symbol.getIdentifier(), symbol.getIdentifier()));

            int shift = ASMUtils.getVariableReferenceShift(function.getReturnType(), node.getNextExpression());
            this.output.append("""
                    \t ADD     R9, R13, #%s ; Store the return value address in R0
                    \t LDR     R0, [R9] ; Load the return value in R0
                    """.formatted(type.getSize() - 4 - shift));

            this.output.append("""
                    \t ADD     R13, R13, #%s ; Remove arguments and return value from stack
                    """.formatted(symbol.getParametersSize() + type.getSize()));
        } else {
            this.output.append(findAddress(node.getIdentifier()));
            this.output.append("""
                    \t BL      %s ; Branch link to %s (it will save the return address in LR)
                    """.formatted(symbol.getIdentifier(), symbol.getIdentifier()));
            this.output.append("""
                    \t ADD     R13, R13, #%s ; Remove arguments and return value from stack
                    """.formatted(symbol.getParametersSize()));
        }
    }

    @Override
    public void visit(IfStatementNode node) throws Exception {
        String ifTrueLabel = "if_true_" + Context.background().getUniqueLabelId();
        String ifFalseLabel = "if_false_" + Context.background().getUniqueLabelId();
        String ifEndLabel = "if_end_" + Context.background().getUniqueLabelId();

        node.getCondition().accept(this);

        this.output.append("""
                \t CMP     R0, #1 ; Compare condition
                \t BEQ    %s ; Branch if condition is true
                """.formatted(ifTrueLabel));


        output.append("\t B       ").append(ifFalseLabel).append("\n");


        output.append(ifTrueLabel).append("\n");

        node.getThenBranch().accept(this);

        output.append("\t B       ").append(ifEndLabel).append("\n");

        output.append(ifFalseLabel).append("\n");

        for (ElseIfStatementNode elseIfNode : node.getElseIfBranches()) {
            elseIfNode.getCondition().accept(this);

            String elseIfTrueLabel = "elsif_true_" + Context.background().getUniqueLabelId();
            String elseIfFalseLabel = "elsif_false_" + Context.background().getUniqueLabelId();

            output.append("""
                    \t CMP     R0, #1 ; Compare condition
                    \t BEQ    %s ; Branch if condition is true
                    \t B       %s ; Branch if condition is false
                    """.formatted(elseIfTrueLabel, elseIfFalseLabel));

            output.append(elseIfTrueLabel).append("\n");

            elseIfNode.getThenBranch().accept(this);

            output.append("\t B       ").append(ifEndLabel).append("\n");

            output.append(elseIfFalseLabel).append("\n");
        }

        if (node.getElseBranch() != null) {
            node.getElseBranch().accept(this);
        }

        output.append(ifEndLabel).append("\n");
    }

    @Override
    public void visit(ElseIfStatementNode elseIfStatementNode) throws Exception {

    }


    @Override
    public void visit(LoopStatementNode node) throws Exception {
        String loopStartLabel = "loop_start_" + Context.background().getUniqueLabelId();
        String loopEndLabel = "loop_end_" + Context.background().getUniqueLabelId();
        if (node.isReverse()) {
            node.getEndExpression().accept(this); // store result in R0
        } else {
            node.getStartExpression().accept(this); // store result in R0
        }


        this.output.append(findVariableAddress(node.getIdentifier(), null)); // store in address in R9

        this.output.append("""   
                \t STR     R0, [R9] ; Assign start expression (assuming result is in R0) to loop variable %s
                """.formatted(node.getIdentifier()));

        output.append(loopStartLabel).append("\n");
        this.output.append("""
                \t LDR     R1, [R9] ; Load variable %s in R1
                """.formatted(node.getIdentifier()));
        if (node.isReverse()) {
            node.getStartExpression().accept(this); // store result in R0
        } else {
            node.getEndExpression().accept(this); // store result in R0
        }

        this.output.append("""
                \t CMP     R1, R0 ; Compare loop variable to end expression
                """);
        if (node.isReverse()) {
            this.output.append("""
                    \t BLT     %s ; Branch if loop variable is less than end expression
                    """.formatted(loopEndLabel));
        } else {
            this.output.append("""
                    \t BGT     %s ; Branch if loop variable is greater than end expression
                    """.formatted(loopEndLabel));
        }

        node.getBody().accept(this);

        this.output.append(findVariableAddress(node.getIdentifier(), null)); // store in address in R9
        this.output.append("\t LDR     R0, [R9] ; Load loop variable in R0\n");

        if (node.isReverse()) {
            this.output.append("\t SUB     R0, R0, #1 ; Decrement loop variable\n");
        } else {
            this.output.append("\t ADD     R0, R0, #1 ; Increment loop variable\n");
        }
        this.output.append("\t STR     R0, [R9] ; Assign incremented loop variable to loop variable\n");

        output.append("\t B       ").append(loopStartLabel).append("\n");

        output.append(loopEndLabel).append("\n");

    }

    @Override
    public void visit(ReturnStatementNode node) throws Exception {
        if (node.getExpression() != null) {
            node.getExpression().accept(this);
            Type type = (Type) scopeStack.findSymbolInScopes(node.getExpression().getType(scopeStack));
            int shift = 2 * 4 + type.getSize();
            if (type instanceof Record record) {
                this.output.append("""
                        \t MOV     R1, R9 ; Load field in R0
                        \t ADD   R2, R11, #%s ; Store return value for in stack-frame
                        """.formatted(shift));
                this.output.append(saveRecordInStack(record, 0));
            } else {
                this.output.append("""
                        \t STR     R0, [R11, #%s] ; Store return value for in stack-frame
                        """.formatted(shift));
            }
        }
        this.output.append("""
                \t MOV     R13, R11 ; Restore frame pointer
                \t LDR     R11, [R13] ; Restore caller's frame pointer
                \t ADD     R13, R13, #4 ;
                \t LDMFD   R13!, {R10, PC} ; Restore caller's frame pointer and return ASM address
                """);

    }

    @Override
    public void visit(WhileStatementNode node) throws Exception {
        //TODO move loop logic and condition inside their respective visits
        String loopStartLabel = "loop_start_" + Context.background().getUniqueLabelId();
        String loopEndLabel = "loop_end_" + Context.background().getUniqueLabelId();

        output.append(loopStartLabel).append("\n");

        node.getCondition().accept(this);

        this.output.append("""
                \t CMP     R0, #0 ; Compare condition
                \t BEQ     %s ; Branch if condition is false
                """.formatted(loopEndLabel));

        node.getBody().accept(this);

        output.append("\t B       ").append(loopStartLabel).append("\n");

        output.append(loopEndLabel).append("\n");
    }

    @Override
    public void visit(BinaryExpressionNode node) throws Exception {

        if (((Type) scopeStack.findSymbolInScopes(node.getLeft().getType(scopeStack))) instanceof Record record) {
            this.output.append("""
                    \t MOV     R0, #1 ; Set R0 to 0
                    """);
            this.output.append(equalsRecord(record, ((VariableReferenceNode) node.getLeft()).getIdentifier(), ((VariableReferenceNode) node.getRight()).getIdentifier(), 0));
            return;

        }

        node.getRight().accept(this);
        this.output.append("""
                \t STMFD   R13!, {R0} ; Store the right operand in the stack
                """);
        node.getLeft().accept(this); // store in R0
        this.output.append("""
                \t LDMFD   R13!, {R1} ; Load the right operand in R1
                """);
        switch (node.getOperatorNode().getOperator()) {
            case ADD -> output.append("\t ADD     R0, R0, R1 ; Add operands\n");
            case SUB -> output.append("\t SUB     R0, R0, R1 ; Sub operands\n");
            case EQUALS -> output.append("""
                    \t CMP     R0, R1 ; Compare operands
                    \t MOVEQ   R0, #1 ; Set R0 to 1 if operands are equal
                    \t MOVNE   R0, #0 ; Set R0 to 0 if operands are not equal
                    """);
            case NOT_EQUALS -> output.append("""
                    \t CMP     R0, R1 ; Compare operands
                    \t MOVNE   R0, #1 ; Set R0 to 1 if operands are not equal
                    \t MOVEQ   R0, #0 ; Set R0 to 0 if operands are equal
                    """);
            case OR -> output.append("""
                    \t ORR     R0, R0, R1 ; Logical OR operands
                    """);
            case AND -> output.append("""
                    \t AND     R0, R0, R1 ; Logical AND operands
                    """);
            case LESS_THAN -> output.append("""
                    \t CMP     R0, R1 ; Compare operands
                    \t MOVLT   R0, #1 ; Set R0 to 1 if R0 is less than R1
                    \t MOVGE   R0, #0 ; Set R0 to 0 if R0 is greater than or equal to R1
                    """);
            case GREATER_THAN -> output.append("""
                    \t CMP     R0, R1 ; Compare operands
                    \t MOVGT   R0, #1 ; Set R0 to 1 if R0 is greater than R1
                    \t MOVLE   R0, #0 ; Set R0 to 0 if R0 is less than or equal to R1
                    """);
            case LESS_THAN_OR_EQUAL -> output.append("""
                    \t CMP     R0, R1 ; Compare operands
                    \t MOVLE   R0, #1 ; Set R0 to 1 if R0 is less than or equal to R1
                    \t MOVGT   R0, #0 ; Set R0 to 0 if R0 is greater than R1
                    """);
            case GREATER_THAN_OR_EQUAL -> output.append("""
                    \t CMP     R0, R1 ; Compare operands
                    \t MOVGE   R0, #1 ; Set R0 to 1 if R0 is greater than or equal to R1
                    \t MOVLT   R0, #0 ; Set R0 to 0 if R0 is less than R1
                    """);
            case MULTIPLY -> {
                output.append("""
                        \t MOV     R2, R0 ; Move R0 to R2
                        \t MOV     R0, #0 ; Clear R0
                        """);
                createMultiplyLoop();
            }
            case DIVIDE -> {
                output.append("""
                        \t MOV     R2, R0 ; Move R0 to R2
                        \t MOV     R0, #0 ; Clear R0
                        """);
                createDivideLoop();
            }
            case MODULO -> {
                output.append("""
                        \t MOV     R2, R0 ; Move R0 to R2
                        \t MOV     R0, #0 ; Clear R0
                        """);
                createRemLoop();
            }
        }
    }

    @Override
    public void visit(CharacterValExpressionNode node) throws Exception {
        node.getExpression().accept(this);
    }

    @Override
    public void visit(LiteralNode node) {
        if (node.getType(scopeStack).equals(TypeEnum.BOOL.toString())) {
            int bool = (boolean) node.getValue() ? 1 : 0;
            this.output.append("\t MOV     R0, #%s ; Load literal value in R0\n".formatted(bool));
        } else if (node.getType(scopeStack).equals(TypeEnum.CHAR.toString())) {
            this.output.append("\t LDR     R0, =%s ; Load literal value in R0\n".formatted((int) String.valueOf(node.getValue()).charAt(0)));
        } else {
            this.output.append("\t LDR     R0, =%s ; Load literal value in R0\n".formatted(node.getValue()));
        }
    }

    @Override
    public void visit(VariableReferenceNode node) throws Exception {
        if (scopeStack.findSymbolInScopes(node.getIdentifier()) instanceof Function) {
            CallNode callNode = new CallNode();
            callNode.setIdentifier(node.getIdentifier());
            callNode.setNextExpression(node.getNextExpression());
            callNode.accept(this);
            return;
        }
        this.output.append(findVariableAddress(node.getIdentifier(), node.getNextExpression()));
        this.output.append("""
                \t LDR     R0, [R9] ; Load variable %s in R0
                """.formatted(node.getIdentifier()));

    }


    @Override
    public void visit(NewExpressionNode node) throws UndefinedVariableException {

    }

    @Override
    public void visit(UnaryExpressionNode node) throws Exception {
        node.getOperand().accept(this);
        switch (node.getOperatorNode().getOperator()) {
            case NOT -> this.output.append("""
                    \t EOR     R0, R0, #1 ; Logical NOT operand
                    """);
            case SUB -> this.output.append("""
                    \t RSBS    R0, R0, #0 ; Negate operand
                    """);
        }

    }

    @Override
    public void visit(TypeNode node) throws Exception {
        // TODO FINAL NODE
    }

    @Override
    public void visit(ProgramNode node) {
        enterScope();
        // TODO Find a way to do this clearly and not to get var decl at the bottom
        try {
            // INIT ALEX LIB
            this.output.append("STR_OUT      FILL    0x1000\n");

            Symbol symbol = scopeStack.findSymbolInScopes(node.getRootProcedure().getIdentifier());
            this.output.append(symbol.getIdentifier()).append("\n").append("""
                    \t STMFD   R13!, {R10, LR} ; Save caller's frame pointer and return ASM address
                    \t MOV     R10, R13 ; Set up new static link
                    \t SUB     R13, R13, #4
                    \t STR     R11, [R13]
                    \t MOV     R11, R13 ; Set up new frame pointer
                    """);

            List<DeclarationNode> tempDeclarations = new ArrayList<>();
            node.getRootProcedure().getBody().getDeclarations().forEach(declaration -> {
                try {
                    if (declaration instanceof ProcedureDeclarationNode || declaration instanceof FunctionDeclarationNode) {
                        tempDeclarations.add(declaration);
                    } else {
                        declaration.accept(this);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            node.getRootProcedure().getBody().getStatements().forEach(statement -> {
                try {
                    statement.accept(this);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            this.output.append("\t END     ; Program ends here\n");

            tempDeclarations.forEach(declaration -> {
                try {
                    declaration.accept(this);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            this.output.append("""
                     print
                     \t STMFD   SP!, {LR, R0-R3}
                     \t MOV     R3, R0
                     \t LDR     R1, =STR_OUT ; address of the output buffer
                     PRINT_LOOP
                     \t LDRB    R2, [R0], #1
                     \t STRB    R2, [R1], #1
                     \t TST     R2, R2
                     \t BNE     PRINT_LOOP
                     \t MOV     R2, #0
                     \t STRB    R2, [R1, #-1]
                     \t MOV     R2, #0
                     \t STRB    R2, [R1]
                                        \s
                     ;  we need to clear the output buffer
                     \t LDR     R1, =STR_OUT
                     \t MOV     R0, R3
                     \t B       CLEAN
                    \s""");

            this.output.append("""
                     println
                     \t STMFD   SP!, {LR, R0-R3}
                     \t MOV     R3, R0
                     \t LDR     R1, =STR_OUT ; address of the output buffer
                     PRINTLN_LOOP
                     \t LDRB    R2, [R0], #1
                     \t STRB    R2, [R1], #1
                     \t TST     R2, R2
                     \t BNE     PRINTLN_LOOP
                     \t MOV     R2, #10
                     \t STRB    R2, [R1, #-1]
                     \t MOV     R2, #0
                     \t STRB    R2, [R1]
                                        \s
                     ;  we need to clear the output buffer
                     \t LDR     R1, =STR_OUT
                     \t MOV     R0, R3
                     CLEAN
                     \t LDRB    R2, [R0], #1
                     \t MOV     R3, #0
                     \t STRB    R3, [R1], #1
                     \t TST     R2, R2
                     \t BNE     CLEAN
                     ;  clear 3 more
                     \t STRB    R3, [R1], #1
                     \t STRB    R3, [R1], #1
                     \t STRB    R3, [R1], #1
                                        \s
                     \t LDMFD   SP!, {PC, R0-R3}
                    \s""");

            this.output.append("""
                     to_ascii
                     \t STMFD   SP!, {LR, R4-R7}
                     \t ; make it positive
                     \t MOV R7, R0
                     \t CMP     R0, #0
                     \t MOVGE   R6, R0
                     \t RSBLT   R6, R0, #0
                     \t MOV     R0, R6
                                        \s
                     \t MOV     R4, #0 ; Initialize digit counter
                                        \s
                     to_ascii_loop\s
                     \t MOV     R1, R0
                     \t MOV     R2, #10
                     \t BL      div32 ; R0 = R0 / 10, R1 = R0 % 10
                     \t ADD     R1, R1, #48 ; Convert digit to ASCII
                     \t STRB    R1, [R3, R4] ; Store the ASCII digit
                     \t ADD     R4, R4, #1 ; Increment digit counter
                     \t CMP     R0, #0
                     \t BNE     to_ascii_loop
                                        \s
                     \t ; add the sign if it was negative
                     \t CMP     R7, #0
                     \t MOVGE   R1, #0\t
                     \t MOVLT   R1, #45
                     \t STRB    R1, [R3, R4]
                     \t ADD     R4, R4, #1
                            \s
                     \t LDMFD   SP!, {PC, R4-R7}
                                        \s
                    \s""");

            this.output.append("""
                    ;       Integer division routine
                    ;       Arguments:
                    ;       R1 = Dividend
                    ;       R2 = Divisor
                    ;       Returns:
                    ;       R0 = Quotient
                    ;       R1 = Remainder
                    div32
                    \t STMFD   SP!, {LR, R2-R5}
                    \t MOV     R0, #0
                    \t MOV     R3, #0
                    \t CMP     R1, #0
                    \t RSBLT   R1, R1, #0
                    \t EORLT   R3, R3, #1
                    \t CMP     R2, #0
                    \t RSBLT   R2, R2, #0
                    \t EORLT   R3, R3, #1
                    \t MOV     R4, R2
                    \t MOV     R5, #1
                    div_max
                    \t LSL     R4, R4, #1
                    \t LSL     R5, R5, #1
                    \t CMP     R4, R1
                    \t BLE     div_max
                    div_loop
                    \t LSR     R4, R4, #1
                    \t LSR     R5, R5, #1
                    \t CMP     R4,R1
                    \t BGT     div_loop
                    \t ADD     R0, R0, R5
                    \t SUB     R1, R1, R4
                    \t CMP     R1, R2
                    \t BGE     div_loop
                    \t CMP     R3, #1
                    \t BNE     div_exit
                    \t CMP     R1, #0
                    \t ADDNE   R0, R0, #1
                    \t RSB     R0, R0, #0
                    \t RSB     R1, R1, #0
                    \t ADDNE   R1, R1, R2
                    div_exit
                    \t CMP     R0, #0
                    \t ADDEQ   R1, R1, R4
                    \t LDMFD   SP!, {PC, R2-R5}
                    """);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        }
        scopeStack.exitScope();
    }

    @Override
    public void visit(ParameterNode node) throws Exception {
        node.getType().accept(this);
        int shift = Context.background().getCounter() + 3;
        this.output.append("""
                \t LDR     R5, [R11, #4 * %s] ; Load parameter %s in R5
                \t STMFD   R13!, {R5} ; Store parameter %s in stack-frame
                """.formatted(shift, node.getIdentifier(), node.getIdentifier()));
    }

    public void createMultiplyLoop() {
        String multiplyLoopLabel = "multiply_loop_" + Context.background().getUniqueLabelId();
        String multiplyEndLabel = "multiply_end_" + Context.background().getUniqueLabelId();

        output.append("""
                \t MOV     R3, #0  ; Initialize R3 to 0 (to store the sign)
                \t CMP     R1, #0  ; Check if the left operand is negative
                \t RSBMI   R1, R1, #0  ; Take the absolute value of the left operand (N=1)
                \t ADDMI   R3, R3, #1  ; Set R3 to 1 to indicate a negative result (N=1)
                \t CMP     R2, #0  ; Check if the right operand is negative
                \t RSBMI   R2, R2, #0  ; Take the absolute value of the right operand (N=1)
                \t ADDMI   R3, R3, #1  ; Set R3 to 1 to indicate a negative result (if it's not already set) (N=1)
                """);

        output.append(multiplyLoopLabel).append("\n");

        output.append("""
                \t CMP     R2, #0 ; Compare R2 to 0
                \t BEQ     %s ; Branch if R2 is 0
                \t ADD     R0, R0, R1 ; Add R0 to R1
                \t SUB     R2, R2, #1 ; Decrement R2
                \t B       %s ; Branch to loop start
                """.formatted(multiplyEndLabel, multiplyLoopLabel));

        output.append(multiplyEndLabel).append("\n");

        output.append("""
                        \t CMP     R3, #1 ; Check if the result should be negative
                        \t RSBEQ   R0, R0, #0 ; Negate the result if the condition is met
                """);
    }

    public void createDivideLoop() {
        String divideLoopLabel = "divide_loop_" + Context.background().getUniqueLabelId();
        String divideEndLabel = "divide_end_" + Context.background().getUniqueLabelId();

        output.append("""
                \t MOV     R3, #0  ; Initialize R3 to 0 (to store the sign)
                \t CMP     R2, #0  ; Check if the left operand is negative
                \t RSBMI   R2, R2, #0  ; Take the absolute value of the left operand (N=1)
                \t ADDMI   R3, R3, #1  ; Set R3 to 1 to indicate a negative result (N=1)
                \t CMP     R1, #0  ; Check if the right operand is negative
                \t RSBMI   R1, R1, #0  ; Take the absolute value of the right operand (N=1)
                \t ADDMI   R3, R3, #1  ; Set R3 to 1 to indicate a negative result (if it's not already set) (N=1)
                """);

        output.append(divideLoopLabel).append("\n");

        output.append("""
                \t CMP     R2, R1 ; Compare R1 to R2
                \t BLT     %s ; Branch if R1 < R2
                \t SUB     R2, R2, R1 ; Subtract R2 from R1
                \t ADD     R0, R0, #1 ; Increment the result
                \t B       %s ; Branch to loop start
                """.formatted(divideEndLabel, divideLoopLabel));

        output.append(divideEndLabel).append("\n");

        output.append("""
                \t CMP     R3, #1 ; Check if the result should be negative
                \t RSBEQ   R0, R0, #0 ; Negate the result if the condition is met
                """);
    }

    public void createRemLoop() {
        String remLoopLabel = "rem_loop_" + Context.background().getUniqueLabelId();
        String remEndLabel = "rem_end_" + Context.background().getUniqueLabelId();

        output.append("""
                \t CMP     R2, #0  ; Check if the left operand is negative
                \t RSBMI   R2, R2, #0  ; Take the absolute value of the left operand (N=1)
                \t MOVMI   R0, #1  ; Set R0 to 1 to indicate a negative result (N=1)
                \t CMP     R1, #0  ; Check if the right operand is negative
                \t RSBMI   R1, R1, #0  ; Take the absolute value of the right operand (N=1)
                """);

        output.append(remLoopLabel).append("\n");

        output.append("""
                \t CMP     R2, R1 ; Compare R1 to R2
                \t BLT     %s ; Branch if R1 < R2
                \t SUB     R2, R2, R1 ; Subtract R2 from R1
                \t B       %s ; Branch to loop start
                """.formatted(remEndLabel, remLoopLabel));

        output.append(remEndLabel).append("\n");

        output.append("""
                \t CMP     R0, #1 ; Check if the result should be negative
                \t RSBEQ   R0, R2, #0 ; Negate the result if the condition is met
                \t MOVNE   R0, R2 ; Move the remainder to R0
                """);
    }


    public void enterScope() {
        scopeStack.push(symbolTables.get(currentTableIndex));
        currentTableIndex++;
    }


}
