package org.trad.pcl.ast.declaration;

import org.trad.pcl.ast.ParameterNode;
import org.trad.pcl.ast.statement.BlockNode;

import java.util.List;

public class ProcedureDeclarationNode extends DeclarationNode {
    private List<ParameterNode> parameters;
    private BlockNode body;

    public ProcedureDeclarationNode(String name) {
        super(name);
    }

    public void setBody(BlockNode body) {
        this.body = body;
    }

    public void addParameter(ParameterNode parameter) {
        parameters.add(parameter);
        parameter.setParent(this);
    }

    public void addParameters(List<ParameterNode> parameters) {
        for (ParameterNode parameter : parameters) {
            addParameter(parameter);
        }
    }

}