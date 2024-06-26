package org.trad.pcl.ast.declaration;

import org.trad.pcl.Exceptions.Semantic.InvalidEndIdentifierException;
import org.trad.pcl.Exceptions.Semantic.MissingReturnStatementException;
import org.trad.pcl.Services.ErrorService;
import org.trad.pcl.ast.ASTNode;
import org.trad.pcl.ast.ParameterNode;
import org.trad.pcl.ast.statement.BlockNode;
import org.trad.pcl.semantic.ASTNodeVisitor;
import org.trad.pcl.semantic.symbol.Function;
import org.trad.pcl.semantic.symbol.Procedure;
import org.trad.pcl.semantic.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public final class ProcedureDeclarationNode extends ASTNode implements DeclarationNode {
    private List<ParameterNode> parameters;

    private String identifier;

    private String endIdentifier;

    private BlockNode body;

    public ProcedureDeclarationNode() {
        this.parameters = new ArrayList<>();
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setEndIdentifier(String endIdentifier) {
        this.endIdentifier = endIdentifier;
    }

    public String getEndIdentifier() {
        return endIdentifier;
    }

    public void addParameter(ParameterNode parameter) {
        parameters.add(parameter);
    }

    public void addParameters(List<ParameterNode> parameters) {
        for (ParameterNode parameter : parameters) {
            addParameter(parameter);
        }
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public boolean checkHasReturn() {
       return this.body.hasReturn();
    }

    public void setBody(BlockNode body) {
        this.body = body;
    }

    public BlockNode getBody() {
        return body;
    }

    public Symbol toSymbol() {
        Procedure f = new Procedure(this.identifier, 0);
        for (ParameterNode parameter : parameters) {
            f.addParameter(parameter.getType().getIdentifier());
        }
        return f;
    }

    public void checkEndIdentifier(){
        if (this.endIdentifier == null){
            return;
        }
        if(!this.identifier.equals(this.endIdentifier)){
            ErrorService.getInstance().registerSemanticError(new InvalidEndIdentifierException(this.identifier, this.endIdentifier, this.body.getConcernedLine()));
        }
    }


    @Override
    public void accept(ASTNodeVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
