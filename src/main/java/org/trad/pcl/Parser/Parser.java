package org.trad.pcl.Parser;

import org.trad.pcl.Exceptions.Syntax.MissingSemicolonException;
import org.trad.pcl.Exceptions.Syntax.UnexpectedTokenException;
import org.trad.pcl.Exceptions.Syntax.UnexpectedTokenListException;
import org.trad.pcl.Helpers.OperatorEnum;
import org.trad.pcl.Helpers.ParameterMode;
import org.trad.pcl.Helpers.TagHelper;
import org.trad.pcl.Lexer.Lexer;
import org.trad.pcl.Lexer.Tokens.Tag;
import org.trad.pcl.Lexer.Tokens.Token;
import org.trad.pcl.Services.ErrorService;
import org.trad.pcl.annotation.PrintMethodName;
import org.trad.pcl.ast.OperatorNode;
import org.trad.pcl.ast.ParameterNode;
import org.trad.pcl.ast.ProgramNode;
import org.trad.pcl.ast.declaration.*;
import org.trad.pcl.ast.expression.*;
import org.trad.pcl.ast.statement.*;
import org.trad.pcl.ast.type.AccessTypeNode;
import org.trad.pcl.ast.type.RecordTypeNode;
import org.trad.pcl.ast.type.TypeNode;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final ErrorService errorService;
    Lexer lexer;
    private Token currentToken;
    private final boolean isLogger;

    public Parser(Lexer lexer, boolean isLogger) {
        this.lexer = lexer;
        this.errorService = ErrorService.getInstance();
        this.currentToken = lexer.nextToken();
        this.isLogger = isLogger;
    }

    public Parser(Lexer lexer) {
        this(lexer, false);
    }

    public ProgramNode parse() {
        return fichier();
    }

    @PrintMethodName
    private ProgramNode fichier() {
        ProgramNode abstractSyntaxTreeRoot = new ProgramNode();
        analyseTerminal(Tag.WITH);
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.DOT);
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.SEMICOLON);
        analyseTerminal(Tag.USE);
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.DOT);
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.SEMICOLON);
        analyseTerminal(Tag.PROCEDURE);
        ProcedureDeclarationNode rootProcedure = new ProcedureDeclarationNode();
        rootProcedure.setConcernedLine(this.currentToken.line());
        rootProcedure.setIdentifier(analyseTerminal(Tag.IDENT).getValue());
        analyseTerminal(Tag.IS);
        BlockNode block = new BlockNode();
        block.setConcernedLine(this.currentToken.line());
        block.addDeclarations(multipleDeclarations());
        analyseTerminal(Tag.BEGIN);
        block.addStatements(multipleStatements());
        rootProcedure.setBody(block);
        abstractSyntaxTreeRoot.setRootProcedure(rootProcedure);
        analyseTerminal(Tag.END);
        rootProcedure.setEndIdentifier(hasident());
        analyseTerminal(Tag.SEMICOLON);
        analyseTerminal(Tag.EOF);

        return abstractSyntaxTreeRoot;
    }

    /**
     * Grammar rule : decl
     */
    @PrintMethodName
    public List<DeclarationNode> declaration() {
        List<DeclarationNode> declarations = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case PROCEDURE -> {
                ProcedureDeclarationNode declaration = new ProcedureDeclarationNode();
                declaration.setConcernedLine(this.currentToken.line());
                declarations.add(declaration);
                analyseTerminal(Tag.PROCEDURE);
                declaration.setIdentifier(analyseTerminal(Tag.IDENT).getValue());
                declaration.addParameters(hasParameters());
                analyseTerminal(Tag.IS);
                BlockNode block = new BlockNode();
                block.setConcernedLine(this.currentToken.line());
                block.addDeclarations(multipleDeclarations());
                analyseTerminal(Tag.BEGIN);
                block.addStatements(multipleStatements());
                declaration.setBody(block);
                analyseTerminal(Tag.END);
                declaration.setEndIdentifier(hasident());
                analyseTerminal(Tag.SEMICOLON);
            }
            case IDENT -> {
                //declaration.setName(this.currentToken.getValue());
                List<String> idents = multipleIdent();
                analyseTerminal(Tag.COLON);
                TypeNode typeNode = type();
                ExpressionNode assignNode = assignDeclarationExpression();
                for (String ident : idents) {
                    VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode();
                    variableDeclarationNode.setConcernedLine(this.currentToken.line());
                    variableDeclarationNode.setIdentifier(ident);
                    variableDeclarationNode.setType(typeNode);
                    if (assignNode != null) {
                        AssignmentStatementNode assignmentStatementNode = new AssignmentStatementNode();
                        assignmentStatementNode.setConcernedLine(this.currentToken.line());
                        assignmentStatementNode.setIdentifier(variableDeclarationNode.getIdentifier());
                        assignmentStatementNode.setExpression(assignNode);
                        variableDeclarationNode.setAssignment(assignmentStatementNode);
                    }
                    declarations.add(variableDeclarationNode);
                }
                analyseTerminal(Tag.SEMICOLON);
            }
            case TYPE -> {
                TypeDeclarationNode declaration = new TypeDeclarationNode();
                declaration.setConcernedLine(this.currentToken.line());
                declarations.add(declaration);
                analyseTerminal(Tag.TYPE);
                String ident = analyseTerminal(Tag.IDENT).getValue();
                TypeNode type = isAccessOrRecord();
                type.setIdentifier(ident);
                declaration.setType(type);
                analyseTerminal(Tag.SEMICOLON);
            }
            case FUNCTION -> {
                FunctionDeclarationNode declaration = new FunctionDeclarationNode();
                declaration.setConcernedLine(this.currentToken.line());
                declarations.add(declaration);
                analyseTerminal(Tag.FUNCTION);
                declaration.setIdentifier(analyseTerminal(Tag.IDENT).getValue());
                declaration.addParameters(hasParameters());
                analyseTerminal(Tag.RETURN);
                declaration.setReturnType(type());
                analyseTerminal(Tag.IS);
                BlockNode block = new BlockNode();
                block.setConcernedLine(this.currentToken.line());
                block.addDeclarations(multipleDeclarations());
                analyseTerminal(Tag.BEGIN);
                block.addStatements(multipleStatements());
                declaration.setBody(block);
                analyseTerminal(Tag.END);
                declaration.setEndIdentifier(hasident());
                analyseTerminal(Tag.SEMICOLON);
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.PROCEDURE, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.TYPE, this.currentToken),
                            Token.generateExpectedToken(Tag.FUNCTION, this.currentToken))
            );
        }
        return declarations;
    }

    /**
     * Grammar rule : hasischoose
     */
    @PrintMethodName
    private TypeNode isAccessOrRecord() {
        TypeNode type = null;
        switch (this.currentToken.tag()) {
            case IS -> {
                analyseTerminal(Tag.IS);
                type = AccessOrRecord();
            }
            case SEMICOLON -> {
                type = new TypeNode();
                type.setConcernedLine(this.currentToken.line());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IS, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken))
            );
        }
        return type;
    }


    /**
     * Grammar rule : accorrec
     */
    @PrintMethodName
    private TypeNode AccessOrRecord() {
        TypeNode type = null;
        switch (this.currentToken.tag()) {
            case ACCESS -> {
                type = new AccessTypeNode();
                type.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.ACCESS);
                TypeNode simpleTypeNode = new TypeNode();
                simpleTypeNode.setConcernedLine(this.currentToken.line());
                simpleTypeNode.setIdentifier(analyseTerminal(Tag.IDENT).getValue());
                ((AccessTypeNode) type).setBaseType(simpleTypeNode);
            }
            case RECORD -> {
                type = new RecordTypeNode();
                type.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.RECORD);
                ((RecordTypeNode) type).addFields(multipleFields());
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.RECORD);
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.ACCESS, this.currentToken),
                            Token.generateExpectedToken(Tag.RECORD, this.currentToken))
            );
        }
        return type;
    }

    /**
     * Grammar rule : decls
     */
    @PrintMethodName
    private List<DeclarationNode> multipleDeclarations() {
        List<DeclarationNode> declarations = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case PROCEDURE, IDENT, TYPE, FUNCTION -> {
                declarations.addAll(declaration());
                declarations.addAll(multipleDeclarations());
            }
            case BEGIN -> {
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.PROCEDURE, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.TYPE, this.currentToken),
                            Token.generateExpectedToken(Tag.FUNCTION, this.currentToken),
                            Token.generateExpectedToken(Tag.BEGIN, this.currentToken))
            );
        }
        return declarations;
    }

    @PrintMethodName
    private String hasident() {
        String ident = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
            }
            case IDENT -> {
                ident = (analyseTerminal(Tag.IDENT).getValue());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken))
            );
        }
        return ident;
    }

    /**
     * Grammar rule : identsep
     */
    @PrintMethodName
    private List<String> multipleIdent() {
        List<String> identifiers = new ArrayList<>();
        if (this.currentToken.tag() == Tag.IDENT) {
            identifiers.add(analyseTerminal(Tag.IDENT).getValue());
            identifiers.addAll(identSeparator());
        } else {
            this.errorService.registerSyntaxError(new UnexpectedTokenException(Token.generateExpectedToken(Tag.IDENT, this.currentToken), this.currentToken));
        }
        return identifiers;
    }

    /**
     * Grammar rule : identsep2
     */
    @PrintMethodName
    private List<String> identSeparator() {
        List<String> declarations = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case COLON -> {
            }
            case COMMA -> {
                analyseTerminal(Tag.COMMA);
                declarations.addAll(multipleIdent());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.COLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken))
            );
        }
        return declarations;
    }

    /**
     * Grammar rule : champ
     */
    @PrintMethodName
    private List<VariableDeclarationNode> field() {
        List<VariableDeclarationNode> declarations = new ArrayList<>();
        if (this.currentToken.tag() == Tag.IDENT) {
            List<String> idents = multipleIdent();
            analyseTerminal(Tag.COLON);
            TypeNode type = type();
            for (String ident : idents) {
                VariableDeclarationNode declaration = new VariableDeclarationNode();
                declaration.setConcernedLine(this.currentToken.line());
                declaration.setIdentifier(ident);
                declaration.setType(type);
                declarations.add(declaration);
            }
            analyseTerminal(Tag.SEMICOLON);
        } else {
            this.errorService.registerSyntaxError(new UnexpectedTokenException(Token.generateExpectedToken(Tag.IDENT, this.currentToken), this.currentToken));
        }
        return declarations;
    }

    /**
     * Grammar rule : champs
     */
    @PrintMethodName
    private List<VariableDeclarationNode> multipleFields() {
        List<VariableDeclarationNode> declarations = new ArrayList<>();
        if (this.currentToken.tag() == Tag.IDENT) {
            declarations.addAll(field());
            declarations.addAll(fieldsSeparator());
        } else {
            this.errorService.registerSyntaxError(new UnexpectedTokenException(Token.generateExpectedToken(Tag.IDENT, this.currentToken), this.currentToken));
        }
        return declarations;
    }

    /**
     * Grammar rule : champs2
     */
    @PrintMethodName
    private List<VariableDeclarationNode> fieldsSeparator() {
        List<VariableDeclarationNode> declarations = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case IDENT -> declarations.addAll(multipleFields());
            case END -> {
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.END, this.currentToken))
            );
        }
        return declarations;
    }

    /**
     * Grammar rule : type_n
     */
    @PrintMethodName
    private TypeNode type() {
        TypeNode type = null;
        switch (this.currentToken.tag()) {
            case ACCESS -> {
                analyseTerminal(Tag.ACCESS);
                type = new AccessTypeNode();
                type.setConcernedLine(this.currentToken.line());
                TypeNode simpleTypeNode = new TypeNode();
                simpleTypeNode.setConcernedLine(this.currentToken.line());
                simpleTypeNode.setIdentifier(analyseTerminal(Tag.IDENT).getValue());
                ((AccessTypeNode) type).setBaseType(simpleTypeNode);
            }
            case IDENT -> {
                type = new TypeNode();
                type.setConcernedLine(this.currentToken.line());
                type.setIdentifier(analyseTerminal(Tag.IDENT).getValue());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.ACCESS, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken))
            );
        }
        return type;
    }

    /**
     * Grammar rule : params
     */
    @PrintMethodName
    private List<ParameterNode> functionParameters() {
        List<ParameterNode> parameters = new ArrayList<>();
        if (this.currentToken.tag() == Tag.OPEN_PAREN) {
            analyseTerminal(Tag.OPEN_PAREN);
            parameters.addAll(multipleParameters());
            analyseTerminal(Tag.CLOSE_PAREN);
        } else {
            this.errorService.registerSyntaxError(new UnexpectedTokenException(Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken), this.currentToken));
        }
        return parameters;
    }

    /**
     * Grammar rule : hasparams
     */
    @PrintMethodName
    private List<ParameterNode> hasParameters() {
        List<ParameterNode> parameters = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case IS, RETURN -> {
            }
            case OPEN_PAREN -> parameters.addAll(functionParameters());
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IS, this.currentToken),
                            Token.generateExpectedToken(Tag.RETURN, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken))
            );
        }
        return parameters;
    }

    /**
     * Grammar rule : paramsep
     */

    @PrintMethodName
    private List<ParameterNode> multipleParameters() {
        List<ParameterNode> parameters = new ArrayList<>();
        if (this.currentToken.tag() == Tag.IDENT) {
            parameters.addAll(parameter());
            parameters.addAll(paramSeparator());
        } else {
            this.errorService.registerSyntaxError(new UnexpectedTokenException(Token.generateExpectedToken(Tag.IDENT, this.currentToken), this.currentToken));
        }
        return parameters;
    }


    /**
     * Grammar rule : paramsep2
     */
    @PrintMethodName
    private List<ParameterNode> paramSeparator() {
        List<ParameterNode> parameters = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
                analyseTerminal(Tag.SEMICOLON);
                parameters.addAll(multipleParameters());
            }
            case CLOSE_PAREN -> {
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken))
            );
        }
        return parameters;
    }

    /**
     * Grammar rule : typexpr
     */
    @PrintMethodName
    private ExpressionNode assignDeclarationExpression() {
        ExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case ASSIGN -> {
                analyseTerminal(Tag.ASSIGN);
                expression = expression();
            }
            case SEMICOLON -> {
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.ASSIGN, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : param
     */
    @PrintMethodName
    private List<ParameterNode> parameter() {
        List<ParameterNode> parameters = new ArrayList<>();
        if (this.currentToken.tag() == Tag.IDENT) {
            List<String> idents = multipleIdent();
            analyseTerminal(Tag.COLON);
            ParameterMode mode = mode();
            TypeNode type = type();
            for (String ident : idents) {
                ParameterNode parameter = new ParameterNode();
                parameter.setConcernedLine(this.currentToken.line());
                parameter.setIdentifier(ident);
                parameter.setMode(mode);
                parameter.setType(type);
                parameters.add(parameter);
            }
        } else {
            this.errorService.registerSyntaxError(new UnexpectedTokenException(new Token(Tag.IDENT, this.currentToken.line(), TagHelper.getTagString(Tag.IDENT)), this.currentToken));
        }
        return parameters;
    }

    @PrintMethodName
    private ParameterMode mode() {
        ParameterMode mode = null;
        switch (this.currentToken.tag()) {
            case IDENT, ACCESS -> {
            }
            case IN -> {
                analyseTerminal(Tag.IN);
                mode = modeout();
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IN, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.ACCESS, this.currentToken))
            );

        }
        return mode;
    }

    @PrintMethodName
    private ParameterMode modeout() {
        ParameterMode mode = null;
        switch (this.currentToken.tag()) {
            case IDENT, ACCESS -> {
            }
            case OUT -> {
                analyseTerminal(Tag.OUT);
                mode = ParameterMode.INOUT;
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.OUT, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.ACCESS, this.currentToken))
            );
        }
        return mode;
    }

    /**
     * Grammar rule : expr
     */
    @PrintMethodName
    private ExpressionNode expression() {
        switch (this.currentToken.tag()) {
            case IDENT, MINUS, OPEN_PAREN, DOT, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER, NOT -> {
                return LeftOrExpression();
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.DOT, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );

        }
        return null;
    }

    /**
     * Grammar rule : or_expr
     */
    @PrintMethodName
    private ExpressionNode LeftOrExpression() {
        switch (this.currentToken.tag()) {
            case IDENT, MINUS, OPEN_PAREN, DOT, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER, NOT -> {
                ExpressionNode firstExpression = LeftAndExpression();
                BinaryExpressionNode secondExpression = OrExpression();
                if (secondExpression != null) {
                    secondExpression.setConcernedLine(this.currentToken.line());
                    secondExpression.setMostLeft(firstExpression);
                    return secondExpression;
                } else {
                    return firstExpression;
                }
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.DOT, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );
        }
        return null;
    }

    /**
     * Grammar rule : or_expr2
     *
     * @return BinaryExpressionNode with operator and right expression
     * the left expression is set in LeftOrExpression
     */
    @PrintMethodName
    private BinaryExpressionNode OrExpression() {
        BinaryExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, THEN, DOTDOT, LOOP -> {
            }
            case OR -> {
                analyseTerminal(Tag.OR);
                return RightOrExpression();
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.OR, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.DOTDOT, this.currentToken),
                            Token.generateExpectedToken(Tag.LOOP, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : or_expr3
     */
    @PrintMethodName
    private BinaryExpressionNode RightOrExpression() {
        BinaryExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, DOT, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER, NOT -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                expression.setOperator(OperatorEnum.OR);
                expression.setRight(LeftAndExpression(), OrExpression());
                return expression;
            }
            case ELSE -> {
                analyseTerminal(Tag.ELSE);
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                expression.setOperator(OperatorEnum.ORELSE);
                expression.setRight(LeftAndExpression(), OrExpression());
                return expression;
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.ELSE, this.currentToken),
                            Token.generateExpectedToken(Tag.DOT, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );
        }
        return null;
    }

    /**
     * Grammar rule : and_expr
     */
    @PrintMethodName
    private ExpressionNode LeftAndExpression() {
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER, NOT -> {
                ExpressionNode firstExpression = NotExpression();
                BinaryExpressionNode secondExpression = AndExpression();
                if (secondExpression != null) {
                    secondExpression.setConcernedLine(this.currentToken.line());
                    secondExpression.setMostLeft(firstExpression);
                    return secondExpression;
                } else {
                    return firstExpression;
                }
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );
        }
        return null;
    }

    /**
     * Grammar rule : and_expr2
     *
     * @return BinaryExpressionNode with operator and right expression
     * the left expression is set in LeftAndExpression
     */
    @PrintMethodName
    private BinaryExpressionNode AndExpression() {
        BinaryExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, THEN, DOTDOT, LOOP -> {
            }
            case AND -> {
                analyseTerminal(Tag.AND);
                return RightAndExpression();
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.AND, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.OR, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.DOTDOT, this.currentToken),
                            Token.generateExpectedToken(Tag.LOOP, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : and_expr3
     */
    @PrintMethodName
    private BinaryExpressionNode RightAndExpression() {
        BinaryExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER, NOT -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                expression.setOperator(OperatorEnum.AND);
                expression.setRight(NotExpression(), AndExpression());
            }
            case THEN -> {
                analyseTerminal(Tag.THEN);
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                expression.setOperator(OperatorEnum.ANDTHEN);
                expression.setRight(NotExpression(), AndExpression());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : not_expr
     */
    @PrintMethodName
    private ExpressionNode NotExpression() {
        ExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                expression = LeftEqualityExpression();
            }
            case NOT -> {
                expression = new UnaryExpressionNode();
                OperatorNode operator = new OperatorNode();
                operator.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.NOT);
                operator.setOperator(OperatorEnum.NOT);
                ((UnaryExpressionNode) expression).setOperator(operator);
                ((UnaryExpressionNode) expression).setOperand(primary());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : equality_expr
     *
     * @return if there is an equality expression, return BinaryExpressionNode with
     */
    @PrintMethodName
    private ExpressionNode LeftEqualityExpression() {
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                ExpressionNode firstExpression = LeftRelationalExpression();
                BinaryExpressionNode secondExpression = EqualityExpression();
                if (secondExpression != null) {
                    secondExpression.setConcernedLine(this.currentToken.line());
                    secondExpression.setMostLeft(firstExpression);
                    return secondExpression;
                } else {
                    return firstExpression;
                }
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken))
            );
        }
        return null;
    }

    /**
     * Grammar rule : equality_expr2
     *
     * @return BinaryExpressionNode with operator and right expression
     * the left expression is set in LeftEqualityExpression
     */
    @PrintMethodName
    private BinaryExpressionNode EqualityExpression() {
        BinaryExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, DOTDOT, LOOP -> {
            }
            case EQ -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.EQ);
                expression.setOperator(OperatorEnum.EQUALS);
                expression.setRight(LeftRelationalExpression(), EqualityExpression());
            }
            case NE -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.NE);
                expression.setOperator(OperatorEnum.NOT_EQUALS);
                expression.setRight(LeftRelationalExpression(), EqualityExpression());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.EQ, this.currentToken),
                            Token.generateExpectedToken(Tag.NE, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.OR, this.currentToken),
                            Token.generateExpectedToken(Tag.AND, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken),
                            Token.generateExpectedToken(Tag.DOTDOT, this.currentToken),
                            Token.generateExpectedToken(Tag.LOOP, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : relational_expr
     */
    @PrintMethodName
    private ExpressionNode LeftRelationalExpression() {
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                ExpressionNode firstExpression = LeftAdditiveExpression();
                BinaryExpressionNode secondExpression = RelationalExpression();
                if (secondExpression != null) {
                    secondExpression.setConcernedLine(this.currentToken.line());
                    secondExpression.setMostLeft(firstExpression);
                    return secondExpression;
                } else {
                    return firstExpression;
                }
            }
        }
        return null;
    }

    /**
     * Grammar rule : relational_expr2
     *
     * @return BinaryExpressionNode with operator and right expression
     * the left expression is set in LeftRelationalExpression
     */
    @PrintMethodName
    private BinaryExpressionNode RelationalExpression() {
        BinaryExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, EQ, NE, DOTDOT, LOOP -> {
            }
            case LT -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.LT);
                expression.setOperator(OperatorEnum.LESS_THAN);
                expression.setRight(LeftAdditiveExpression(), RelationalExpression());
            }
            case LE -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.LE);
                expression.setOperator(OperatorEnum.LESS_THAN_OR_EQUAL);
                expression.setRight(LeftAdditiveExpression(), RelationalExpression());
            }
            case GT -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.GT);
                expression.setOperator(OperatorEnum.GREATER_THAN);
                expression.setRight(LeftAdditiveExpression(), RelationalExpression());
            }
            case GE -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.GE);
                expression.setOperator(OperatorEnum.GREATER_THAN_OR_EQUAL);
                expression.setRight(LeftAdditiveExpression(), RelationalExpression());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.LT, this.currentToken),
                            Token.generateExpectedToken(Tag.LE, this.currentToken),
                            Token.generateExpectedToken(Tag.GT, this.currentToken),
                            Token.generateExpectedToken(Tag.GE, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.OR, this.currentToken),
                            Token.generateExpectedToken(Tag.AND, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken),
                            Token.generateExpectedToken(Tag.EQ, this.currentToken),
                            Token.generateExpectedToken(Tag.NE, this.currentToken),
                            Token.generateExpectedToken(Tag.DOTDOT, this.currentToken),
                            Token.generateExpectedToken(Tag.LOOP, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : additive_expr
     */
    @PrintMethodName
    private ExpressionNode LeftAdditiveExpression() {
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                ExpressionNode firstExpression = LeftMultiplicativeExpression();
                BinaryExpressionNode secondExpression = AdditiveExpression();
                if (secondExpression != null) {
                    secondExpression.setConcernedLine(this.currentToken.line());
                    secondExpression.setMostLeft(firstExpression);
                    return secondExpression;
                } else {
                    return firstExpression;
                }
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken))
            );
        }
        return null;
    }

    /**
     * Grammar rule : additive_expr2
     *
     * @return BinaryExpressionNode with operator and right expression
     * the left expression is set in LeftAdditiveExpression
     */
    @PrintMethodName
    private BinaryExpressionNode AdditiveExpression() {
        BinaryExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, EQ, NE, LT, LE, GT, GE, DOTDOT, LOOP -> {
            }
            case PLUS -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.PLUS);
                expression.setOperator(OperatorEnum.ADD);
                ExpressionNode left = LeftMultiplicativeExpression();
                BinaryExpressionNode right = AdditiveExpression();
                if (right != null) {
                    right.setConcernedLine(this.currentToken.line());
                    right.setMostLeft(left);
                    expression.setRight(right);
                } else {
                    expression.setConcernedLine(this.currentToken.line());
                    expression.setRight(left);
                }
            }
            case MINUS -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.MINUS);
                expression.setOperator(OperatorEnum.SUB);
                ExpressionNode left = LeftMultiplicativeExpression();
                BinaryExpressionNode right = AdditiveExpression();
                if (right != null) {
                    right.setConcernedLine(this.currentToken.line());
                    right.setMostLeft(left);
                    expression.setRight(right);
                    expression.definePriority(OperatorEnum.SUB, OperatorEnum.ADD);
                } else {
                    expression.setConcernedLine(this.currentToken.line());
                    expression.setRight(left);
                }
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.PLUS, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.OR, this.currentToken),
                            Token.generateExpectedToken(Tag.AND, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken),
                            Token.generateExpectedToken(Tag.EQ, this.currentToken),
                            Token.generateExpectedToken(Tag.NE, this.currentToken),
                            Token.generateExpectedToken(Tag.LT, this.currentToken),
                            Token.generateExpectedToken(Tag.LE, this.currentToken),
                            Token.generateExpectedToken(Tag.GT, this.currentToken),
                            Token.generateExpectedToken(Tag.GE, this.currentToken),
                            Token.generateExpectedToken(Tag.DOTDOT, this.currentToken),
                            Token.generateExpectedToken(Tag.LOOP, this.currentToken)
                    )
            );
        }
        return expression;
    }

    /**
     * Grammar rule : multiplicative_expr
     */
    @PrintMethodName
    private ExpressionNode LeftMultiplicativeExpression() {
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                ExpressionNode firstExpression = minusExpression();
                BinaryExpressionNode secondExpression = MultiplicativeExpression();
                if (secondExpression != null) {
                    secondExpression.setConcernedLine(this.currentToken.line());
                    secondExpression.setMostLeft(firstExpression);
                    return secondExpression;
                } else {
                    return firstExpression;
                }
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken))
            );
        }
        return null;
    }

    /**
     * Grammar rule : multiplicative_expr2
     *
     * @return BinaryExpressionNode with operator and right expression
     * the left expression is set in LeftMultiplicativeExpression
     */
    @PrintMethodName
    private BinaryExpressionNode MultiplicativeExpression() {
        BinaryExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, EQ, NE, LT, LE, GT, GE, PLUS, MINUS, DOTDOT, LOOP -> {
            }
            case MULTI -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.MULTI);
                expression.setOperator(OperatorEnum.MULTIPLY);
                expression.setRight(minusExpression(), MultiplicativeExpression());
            }
            case DIV -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.DIV);
                expression.setOperator(OperatorEnum.DIVIDE);
                ExpressionNode leftNode = minusExpression();
                BinaryExpressionNode rightTree = MultiplicativeExpression();
                if (rightTree != null) {
                    rightTree.setConcernedLine(this.currentToken.line());
                    rightTree.setMostLeft(leftNode);
                    expression.setRight(rightTree);
                    expression.definePriority(OperatorEnum.DIVIDE, OperatorEnum.MULTIPLY);
                } else {
                    expression.setConcernedLine(this.currentToken.line());
                    expression.setRight(leftNode);
                }
            }
            case REM -> {
                expression = new BinaryExpressionNode();
                expression.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.REM);
                expression.setOperator(OperatorEnum.MODULO);
                expression.setRight(minusExpression(), MultiplicativeExpression());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.MULTI, this.currentToken),
                            Token.generateExpectedToken(Tag.DIV, this.currentToken),
                            Token.generateExpectedToken(Tag.REM, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.OR, this.currentToken),
                            Token.generateExpectedToken(Tag.AND, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken),
                            Token.generateExpectedToken(Tag.EQ, this.currentToken),
                            Token.generateExpectedToken(Tag.NE, this.currentToken),
                            Token.generateExpectedToken(Tag.LT, this.currentToken),
                            Token.generateExpectedToken(Tag.LE, this.currentToken),
                            Token.generateExpectedToken(Tag.GT, this.currentToken),
                            Token.generateExpectedToken(Tag.GE, this.currentToken),
                            Token.generateExpectedToken(Tag.PLUS, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.DOTDOT, this.currentToken),
                            Token.generateExpectedToken(Tag.LOOP, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : unary_expr
     */
    @PrintMethodName
    private ExpressionNode minusExpression() {
        ExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case MINUS -> {
                expression = new UnaryExpressionNode();
                OperatorNode operator = new OperatorNode();
                operator.setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.MINUS);
                operator.setOperator(OperatorEnum.SUB);
                ((UnaryExpressionNode) expression).setOperator(operator);
                ((UnaryExpressionNode) expression).setOperand(primary());
            }
            case IDENT, OPEN_PAREN, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> expression = primary();
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken))
            );
        }
        return expression;
    }

    @PrintMethodName
    private ExpressionNode primary() {
        ExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case IDENT -> {
                if (this.currentToken.getValue().equalsIgnoreCase("character")) {
                    expression = characterMethod();
                } else {
                    String ident = analyseTerminal(Tag.IDENT).getValue();
                    expression = identPrimary();
                    ((IdentifiableExpression) expression).setIdentifier(ident);
                }

            }
            case OPEN_PAREN -> {
                analyseTerminal(Tag.OPEN_PAREN);
                expression = expression();
                analyseTerminal(Tag.CLOSE_PAREN);
            }
            case ENTIER -> {
                expression = new LiteralNode();
                ((LiteralNode) expression).setConcernedLine(this.currentToken.line());
                ((LiteralNode) expression).setValue(Long.parseLong(analyseTerminal(Tag.ENTIER).getValue()));
            }
            case CARACTERE -> {
                expression = new LiteralNode();
                ((LiteralNode) expression).setConcernedLine(this.currentToken.line());
                ((LiteralNode) expression).setValue(analyseTerminal(Tag.CARACTERE).getValue().charAt(0));
            }
            case TRUE -> {
                expression = new LiteralNode();
                ((LiteralNode) expression).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.TRUE);
                ((LiteralNode) expression).setValue(true);

            }
            case FALSE -> {
                expression = new LiteralNode();
                ((LiteralNode) expression).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.FALSE);
                ((LiteralNode) expression).setValue(false);
            }
            case NULL -> {
                expression = new LiteralNode();
                ((LiteralNode) expression).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.NULL);
                ((LiteralNode) expression).setValue(null);
            }
            case NEW -> {
                expression = new NewExpressionNode();
                ((NewExpressionNode) expression).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.NEW);
                ((NewExpressionNode) expression).setIdentifier(analyseTerminal(Tag.IDENT).getValue());
            }
            /*
            case CHARACTER -> {
                expression = new CharacterValExpressionNode();
                analyseTerminal(Tag.CHARACTER);
                analyseTerminal(Tag.APOSTROPHE);
                analyseTerminal(Tag.VAL);
                analyseTerminal(Tag.OPEN_PAREN);
                ((CharacterValExpressionNode) expression).setExpression(expression());
                analyseTerminal(Tag.CLOSE_PAREN);
            }
             */
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken))
            );

        }
        return expression;
    }

    @PrintMethodName
    private ExpressionNode characterMethod() {
        ExpressionNode expression = null;
        String ident = analyseTerminal(Tag.IDENT).getValue();
        switch (this.currentToken.tag()) {
            case APOSTROPHE -> {
                expression = new CharacterValExpressionNode();
                analyseTerminal(Tag.APOSTROPHE);
                analyseTerminal(Tag.VAL);
                analyseTerminal(Tag.OPEN_PAREN);
                ((CharacterValExpressionNode) expression).setExpression(expression());
                ((CharacterValExpressionNode) expression).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.CLOSE_PAREN);
            }
            default -> {
                expression = identPrimary();
                ((VariableReferenceNode) expression).setIdentifier(ident);
                ((VariableReferenceNode) expression).setConcernedLine(this.currentToken.line());
            }
        }
        return expression;
    }

    /**
     * Grammar rule : primary2
     */
    @PrintMethodName
    private VariableReferenceNode identPrimary() {
        VariableReferenceNode expression = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, EQ, NE, LT, LE, GT, GE, PLUS, MINUS, MULTI, DIV, REM, DOTDOT, LOOP, DOT -> {
                expression = new VariableReferenceNode();
                expression.setConcernedLine(this.currentToken.line());
                expression.setNextExpression(acces());
            }
            case OPEN_PAREN -> {
                analyseTerminal(Tag.OPEN_PAREN);
                expression = new CallNode();
                expression.setConcernedLine(this.currentToken.line());
                ((CallNode) expression).setIsExpression(true);
                ((CallNode) expression).setArguments(multipleExpressions());
                analyseTerminal(Tag.CLOSE_PAREN);
                expression.setNextExpression(acces());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.OR, this.currentToken),
                            Token.generateExpectedToken(Tag.AND, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken),
                            Token.generateExpectedToken(Tag.EQ, this.currentToken),
                            Token.generateExpectedToken(Tag.NE, this.currentToken),
                            Token.generateExpectedToken(Tag.LT, this.currentToken),
                            Token.generateExpectedToken(Tag.LE, this.currentToken),
                            Token.generateExpectedToken(Tag.GT, this.currentToken),
                            Token.generateExpectedToken(Tag.GE, this.currentToken),
                            Token.generateExpectedToken(Tag.PLUS, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.MULTI, this.currentToken),
                            Token.generateExpectedToken(Tag.DIV, this.currentToken),
                            Token.generateExpectedToken(Tag.REM, this.currentToken),
                            Token.generateExpectedToken(Tag.DOTDOT, this.currentToken),
                            Token.generateExpectedToken(Tag.LOOP, this.currentToken),
                            Token.generateExpectedToken(Tag.DOT, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : exprsep
     */
    @PrintMethodName
    private List<ExpressionNode> multipleExpressions() {
        List<ExpressionNode> expressions = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER, NOT -> {
                expressions.add(expression());
                expressions.addAll(expressionSeparator());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );
        }
        return expressions;
    }

    /**
     * Grammar rule : exprsep2
     */
    @PrintMethodName
    private List<ExpressionNode> expressionSeparator() {
        List<ExpressionNode> expressions = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case COMMA -> {
                analyseTerminal(Tag.COMMA);
                expressions.addAll(multipleExpressions());
            }
            case CLOSE_PAREN -> {
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken))
            );
        }
        return expressions;
    }

    /**
     * Grammar rule : hasexpr
     */
    @PrintMethodName
    private ExpressionNode hasExpression() {
        ExpressionNode expression = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
            }
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER, NOT -> {
                expression = expression();
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.ASSIGN, this.currentToken),
                            Token.generateExpectedToken(Tag.DOT, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );
        }
        return expression;
    }

    /**
     * Grammar rule : instr
     */
    @PrintMethodName
    public StatementNode statement() {
        StatementNode statement = null;
        switch (this.currentToken.tag()) {
            case IDENT -> {
                // appel de fonction et Assign
                String ident = analyseTerminal(Tag.IDENT).getValue();
                statement = identifiableStatement();
                ((IdentifiableStatement) statement).setIdentifier(ident);
            }
            case BEGIN -> {
                statement = new BlockNode();
                ((BlockNode) statement).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.BEGIN);
                ((BlockNode) statement).addStatements(multipleStatements());
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.SEMICOLON);
            }
            case RETURN -> {
                statement = new ReturnStatementNode();
                ((ReturnStatementNode) statement).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.RETURN);
                ((ReturnStatementNode) statement).addExpression(hasExpression());
                analyseTerminal(Tag.SEMICOLON);
            }
            case IF -> {
                statement = new IfStatementNode();
                ((IfStatementNode) statement).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.IF);
                ((IfStatementNode) statement).setCondition(expression());
                analyseTerminal(Tag.THEN);
                BlockNode thenBranch = new BlockNode();
                thenBranch.setConcernedLine(this.currentToken.line());
                thenBranch.addStatements(multipleStatements());
                ((IfStatementNode) statement).setThenBranch(thenBranch);
                ((IfStatementNode) statement).setElseIfBranch(elifn());
                ((IfStatementNode) statement).setElseBranch(elsen());
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.IF);
                analyseTerminal(Tag.SEMICOLON);
            }
            // For var in reverse expr .. expr loop  end loop;
            case FOR -> {
                statement = new LoopStatementNode();
                ((LoopStatementNode) statement).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.FOR);
                ((LoopStatementNode) statement).setIdentifier(analyseTerminal(Tag.IDENT).getValue());
                analyseTerminal(Tag.IN);
                ((LoopStatementNode) statement).setReverse(hasreverse());
                ((LoopStatementNode) statement).setStartExpression(expression());
                analyseTerminal(Tag.DOTDOT);
                ((LoopStatementNode) statement).setEndExpression(expression());
                analyseTerminal(Tag.LOOP);
                BlockNode loopBody = new BlockNode();
                loopBody.setConcernedLine(this.currentToken.line());
                loopBody.addStatements(multipleStatements());
                ((LoopStatementNode) statement).setBody(loopBody);
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.LOOP);
                analyseTerminal(Tag.SEMICOLON);
            }
            case WHILE -> {
                statement = new WhileStatementNode();
                ((WhileStatementNode) statement).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.WHILE);
                ((WhileStatementNode) statement).setCondition(expression());
                analyseTerminal(Tag.LOOP);
                BlockNode loopBody = new BlockNode();
                loopBody.setConcernedLine(this.currentToken.line());
                loopBody.addStatements(multipleStatements());
                ((WhileStatementNode) statement).setBody(loopBody);
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.LOOP);
                analyseTerminal(Tag.SEMICOLON);
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.BEGIN, this.currentToken),
                            Token.generateExpectedToken(Tag.RETURN, this.currentToken),
                            Token.generateExpectedToken(Tag.IF, this.currentToken),
                            Token.generateExpectedToken(Tag.FOR, this.currentToken),
                            Token.generateExpectedToken(Tag.WHILE, this.currentToken))
            );
        }
        return statement;
    }

    /**
     * Grammar rule : instr2
     */
    @PrintMethodName
    private IdentifiableStatement identifiableStatement() {
        IdentifiableStatement statement = null;
        switch (this.currentToken.tag()) {

            case SEMICOLON -> {
                statement = new CallNode();
                ((CallNode) statement).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.SEMICOLON);
            }

            case OPEN_PAREN -> {
                statement = new CallNode();
                ((CallNode) statement).setConcernedLine(this.currentToken.line());
                analyseTerminal(Tag.OPEN_PAREN);
                ((CallNode) statement).setArguments(multipleExpressions());
                analyseTerminal(Tag.CLOSE_PAREN);
                // ignore
                instr3();
                hasassign();
                analyseTerminal(Tag.SEMICOLON);
            }
            case ASSIGN, DOT -> {
                statement = new AssignmentStatementNode();
                ((AssignmentStatementNode) statement).setConcernedLine(this.currentToken.line());
                ((AssignmentStatementNode) statement).getVariableReference().setConcernedLine(this.currentToken.line());
                ((AssignmentStatementNode) statement).getVariableReference().setNextExpression(instr3());
                analyseTerminal(Tag.ASSIGN);
                ((AssignmentStatementNode) statement).setExpression(expression());
                analyseTerminal(Tag.SEMICOLON);
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.ASSIGN, this.currentToken),
                            Token.generateExpectedToken(Tag.DOT, this.currentToken))
            );
        }
        return statement;
    }

    @PrintMethodName
    private VariableReferenceNode instr3() {
        VariableReferenceNode accessReferenceNode = null;
        switch (this.currentToken.tag()) {
            case ASSIGN, SEMICOLON -> {
            }
            case DOT -> {
                analyseTerminal(Tag.DOT);
                accessReferenceNode = new VariableReferenceNode();
                accessReferenceNode.setConcernedLine(this.currentToken.line());
                accessReferenceNode.setIdentifier(analyseTerminal(Tag.IDENT).getValue());
                accessReferenceNode.setNextExpression(instr3());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.ASSIGN, this.currentToken),
                            Token.generateExpectedToken(Tag.DOT, this.currentToken))
            );
        }
        return accessReferenceNode;
    }

    @PrintMethodName
    private void hasassign() {
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
            }
            case ASSIGN -> {
                analyseTerminal(Tag.ASSIGN);
                expression();
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.ASSIGN, this.currentToken))
            );
        }
    }

    @PrintMethodName
    private List<ElseIfStatementNode> elifn() {
        List<ElseIfStatementNode> elseIfStatementNodes = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case END, ELSE -> {
            }
            case ELSIF -> {
                analyseTerminal(Tag.ELSIF);
                ElseIfStatementNode elseIfStatementNode = new ElseIfStatementNode();
                elseIfStatementNode.setConcernedLine(this.currentToken.line());
                elseIfStatementNode.setCondition(expression());
                analyseTerminal(Tag.THEN);
                BlockNode thenBranch = new BlockNode();
                thenBranch.setConcernedLine(this.currentToken.line());
                thenBranch.addStatements(multipleStatements());
                elseIfStatementNode.setThenBranch(thenBranch);
                elseIfStatementNodes.add(elseIfStatementNode);
                elseIfStatementNodes.addAll(elifn());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.ELSIF, this.currentToken),
                            Token.generateExpectedToken(Tag.END, this.currentToken),
                            Token.generateExpectedToken(Tag.ELSE, this.currentToken))
            );
        }
        return elseIfStatementNodes;
    }

    @PrintMethodName
    private BlockNode elsen() {
        BlockNode block = null;
        switch (this.currentToken.tag()) {
            case END -> {
            }
            case ELSE -> {
                analyseTerminal(Tag.ELSE);
                block = new BlockNode();
                block.setConcernedLine(this.currentToken.line());
                block.addStatements(multipleStatements());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.ELSE, this.currentToken),
                            Token.generateExpectedToken(Tag.END, this.currentToken))
            );
        }
        return block;
    }

    @PrintMethodName
    private boolean hasreverse() {
        boolean hasReverse = false;
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER, NOT -> {
            }
            case REVERSE -> {
                analyseTerminal(Tag.REVERSE);
                hasReverse = true;
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.REVERSE, this.currentToken),
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.OPEN_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.ENTIER, this.currentToken),
                            Token.generateExpectedToken(Tag.CARACTERE, this.currentToken),
                            Token.generateExpectedToken(Tag.TRUE, this.currentToken),
                            Token.generateExpectedToken(Tag.FALSE, this.currentToken),
                            Token.generateExpectedToken(Tag.NULL, this.currentToken),
                            Token.generateExpectedToken(Tag.NEW, this.currentToken),
                            Token.generateExpectedToken(Tag.CHARACTER, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken))
            );
        }
        return hasReverse;
    }

    /**
     * Grammar rule : instrs
     */
    @PrintMethodName
    private List<StatementNode> multipleStatements() {
        List<StatementNode> statements = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case IDENT, BEGIN, RETURN, IF, FOR, WHILE -> {
                statements.add(statement());
                statements.addAll(instrs2());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.BEGIN, this.currentToken),
                            Token.generateExpectedToken(Tag.RETURN, this.currentToken),
                            Token.generateExpectedToken(Tag.IF, this.currentToken),
                            Token.generateExpectedToken(Tag.FOR, this.currentToken),
                            Token.generateExpectedToken(Tag.WHILE, this.currentToken))
            );
        }
        return statements;
    }

    @PrintMethodName
    private List<StatementNode> instrs2() {
        List<StatementNode> statements = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case IDENT, BEGIN, RETURN, IF, FOR, WHILE -> {
                statements.add(statement());
                statements.addAll(instrs2());
            }
            case END, ELSE, ELSIF -> {
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.IDENT, this.currentToken),
                            Token.generateExpectedToken(Tag.BEGIN, this.currentToken),
                            Token.generateExpectedToken(Tag.RETURN, this.currentToken),
                            Token.generateExpectedToken(Tag.IF, this.currentToken),
                            Token.generateExpectedToken(Tag.FOR, this.currentToken),
                            Token.generateExpectedToken(Tag.WHILE, this.currentToken),
                            Token.generateExpectedToken(Tag.END, this.currentToken),
                            Token.generateExpectedToken(Tag.ELSE, this.currentToken),
                            Token.generateExpectedToken(Tag.ELSIF, this.currentToken))
            );
        }
        return statements;
    }

    @PrintMethodName
    private VariableReferenceNode acces() {
        VariableReferenceNode variableReferenceNode = null;
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, END, THEN, NOT, EQ, NE, LT, LE, GT, GE, PLUS, MINUS, MULTI, DIV, REM, DOTDOT, LOOP -> {
            }
            case DOT -> {
                analyseTerminal(Tag.DOT);
                variableReferenceNode = new VariableReferenceNode();
                variableReferenceNode.setConcernedLine(this.currentToken.line());
                variableReferenceNode.setIdentifier(analyseTerminal(Tag.IDENT).getValue());
                variableReferenceNode.setNextExpression(acces());
            }
            default -> this.errorService.registerSyntaxError(
                    new UnexpectedTokenListException(this.currentToken,
                            Token.generateExpectedToken(Tag.DOT, this.currentToken),
                            Token.generateExpectedToken(Tag.SEMICOLON, this.currentToken),
                            Token.generateExpectedToken(Tag.COMMA, this.currentToken),
                            Token.generateExpectedToken(Tag.CLOSE_PAREN, this.currentToken),
                            Token.generateExpectedToken(Tag.OR, this.currentToken),
                            Token.generateExpectedToken(Tag.END, this.currentToken),
                            Token.generateExpectedToken(Tag.THEN, this.currentToken),
                            Token.generateExpectedToken(Tag.NOT, this.currentToken),
                            Token.generateExpectedToken(Tag.EQ, this.currentToken),
                            Token.generateExpectedToken(Tag.NE, this.currentToken),
                            Token.generateExpectedToken(Tag.LT, this.currentToken),
                            Token.generateExpectedToken(Tag.LE, this.currentToken),
                            Token.generateExpectedToken(Tag.GT, this.currentToken),
                            Token.generateExpectedToken(Tag.GE, this.currentToken),
                            Token.generateExpectedToken(Tag.PLUS, this.currentToken),
                            Token.generateExpectedToken(Tag.MINUS, this.currentToken),
                            Token.generateExpectedToken(Tag.MULTI, this.currentToken),
                            Token.generateExpectedToken(Tag.DIV, this.currentToken),
                            Token.generateExpectedToken(Tag.REM, this.currentToken),
                            Token.generateExpectedToken(Tag.DOTDOT, this.currentToken),
                            Token.generateExpectedToken(Tag.LOOP, this.currentToken))
            );
        }
        return variableReferenceNode;
    }

    @PrintMethodName
    private Token analyseTerminal(Tag tag) {
        if (isLogger) {
            System.out.println("\t\t↪️ " + this.currentToken);
        }
        if (!(this.currentToken.tag() == tag)) {
            Token expectedToken = new Token(tag, this.currentToken.line(), TagHelper.getTagString(tag));
            if (expectedToken.tag() == Tag.SEMICOLON) {
                this.errorService.registerSyntaxWarning(new MissingSemicolonException(this.currentToken));
            } else {
                this.errorService.registerSyntaxError(new UnexpectedTokenException(expectedToken, this.currentToken));
            }
        }
        // Contient le prochain token ou <EOF, currentLine,""> si fin de fichier
        if (this.currentToken.tag() == Tag.EOF) {
            return this.currentToken;
        }
        Token temp = this.currentToken;
        this.currentToken = lexer.nextToken();
        return temp;
    }

    public void setCurrentToken(Token token) {
        this.currentToken = token;
    }
}
