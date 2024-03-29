package org.trad.pcl.ast.declaration;

import org.trad.pcl.ast.ASTNode;
import org.trad.pcl.ast.ParameterNode;
import org.trad.pcl.ast.statement.BlockNode;
import org.trad.pcl.semantic.ASTNodeVisitor;
import org.trad.pcl.semantic.symbol.Procedure;
import org.trad.pcl.semantic.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public final class ProcedureDeclarationNode extends ASTNode implements DeclarationNode {
    private List<ParameterNode> parameters;

    private String identifier;

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

    /*public void initTDS(SymbolTable tdsBefore) {
        body.initTDS(tdsBefore);
        SymbolTable tds = body.getTDS();
        for (ParameterNode parameter : parameters) {
            tds.addSymbol(parameter.toSymbol());
        }
    }

    public void displayTDS() {
        System.out.println("TDS pour la fonction : " + this.identifier);
        this.body.displayTDS();
    }*/

    public void setBody(BlockNode body) {
        this.body = body;
    }

    public BlockNode getBody() {
        return body;
    }

    public Symbol toSymbol() {
        return new Procedure(this.identifier, 0);
    }


    @Override
    public void accept(ASTNodeVisitor visitor) {
        visitor.visit(this);
    }
}
