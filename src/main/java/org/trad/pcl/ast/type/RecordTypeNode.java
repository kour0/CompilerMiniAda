package org.trad.pcl.ast.type;

import org.trad.pcl.ast.declaration.VariableDeclarationNode;

import java.util.ArrayList;
import java.util.List;

public final class RecordTypeNode extends TypeNode {
    private List<VariableDeclarationNode> fields;

    public RecordTypeNode() {
        super();
        fields = new ArrayList<>();
    }

    public void addField(VariableDeclarationNode field) {
        fields.add(field);
    }

    public void addFields(List<VariableDeclarationNode> fields) {
        for (VariableDeclarationNode field : fields) {
            addField(field);
        }
    }

    public List<VariableDeclarationNode> getFields() {
        return fields;
    }

}
