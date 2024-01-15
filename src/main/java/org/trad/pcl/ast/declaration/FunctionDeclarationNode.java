package org.trad.pcl.ast.declaration;


import org.trad.pcl.ast.ParameterNode;
import org.trad.pcl.ast.statement.BlockNode;
import org.trad.pcl.ast.type.TypeNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionDeclarationNode extends DeclarationNode {
    private List<ParameterNode> parameters;
    private TypeNode returnType;
    private BlockNode body;

    public FunctionDeclarationNode() {
        this.parameters = new ArrayList<>();
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

    public void setReturnType(TypeNode returnType) {
        this.returnType = returnType;
        returnType.setParent(this);
    }

    public void setBody(BlockNode body) {
        this.body = body;
        body.setParent(this);
    }

}
