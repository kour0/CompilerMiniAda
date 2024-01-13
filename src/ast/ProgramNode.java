package ast;

import ast.declaration.ProcedureDeclarationNode;

public class ProgramNode extends ASTNode {
    private ProcedureDeclarationNode rootProcedure;


    public void setRootProcedure(ProcedureDeclarationNode rootProcedure) {
        this.rootProcedure = rootProcedure;
        rootProcedure.setParent(this);
    }

}
