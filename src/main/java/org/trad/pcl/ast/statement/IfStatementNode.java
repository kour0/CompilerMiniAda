package org.trad.pcl.ast.statement;


import org.trad.pcl.ast.expression.ExpressionNode;

public class IfStatementNode extends StatementNode {
    private ExpressionNode condition;
    private BlockNode thenBranch;
    private BlockNode elseBranch;

    public void setCondition(ExpressionNode condition) {
        this.condition = condition;
    }

    public void setThenBranch(BlockNode thenBranch) {
        this.thenBranch = thenBranch;
    }

    public void setElseBranch(BlockNode elseBranch) {
        this.elseBranch = elseBranch;
    }

}
