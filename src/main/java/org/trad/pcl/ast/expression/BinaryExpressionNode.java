package org.trad.pcl.ast.expression;

import org.trad.pcl.ast.ASTNode;
import org.trad.pcl.ast.OperatorNode;
import org.trad.pcl.semantic.ASTNodeVisitor;

public final class BinaryExpressionNode extends ASTNode implements ExpressionNode {
    private ExpressionNode left;
    private OperatorNode operator;
    private ExpressionNode right;


    public void setLeft(ExpressionNode left) {
        this.left = left;
    }

    public void setMostLeft(ExpressionNode left) {
        if (this.left == null) {
            this.left = left;
        } else {
            ((BinaryExpressionNode) this.left).setMostLeft(left);
        }
    }

    public void setRight(ExpressionNode right) {
        this.right = right;
    }

    public void setRight(ExpressionNode first, BinaryExpressionNode second) {
        if (second != null) {
            second.setLeft(first);
            this.right = second;
        } else {
            this.right = first;
        }
    }

    public void setOperator(OperatorNode operator) {
        this.operator = operator;
    }

    public void setOperator(String operator) {
        this.operator = new OperatorNode();
        this.operator.setOperator(operator);
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public ExpressionNode getRight() {
        return right;
    }

    public OperatorNode getOperatorNode() {
        return operator;
    }

    public void rotateLeft() {
        if (this.right == null) {
            throw new IllegalStateException("Right node must not be null for left rotation");
        }

        // Create a copy of the current node
        BinaryExpressionNode copy = new BinaryExpressionNode();
        copy.setLeft(this.left);
        copy.setRight(this.right);
        copy.setOperator(this.operator);

        // Perform the rotation on the copy
        BinaryExpressionNode newParent = (BinaryExpressionNode) copy.right;
        copy.right = newParent.left;
        newParent.left = copy;

        // Set actual tree
        this.left = newParent.left;
        this.right = newParent.right;
        this.operator = newParent.operator;
    }

    public void definePriority(String MostPriorityOperator, String LeastPriorityOperator) {
        BinaryExpressionNode copy = this;
        while (copy.getOperatorNode().getOperator().equals(MostPriorityOperator) && (((BinaryExpressionNode) copy.getRight()).getOperatorNode().getOperator().equals(LeastPriorityOperator) || ((BinaryExpressionNode) copy.getRight()).getOperatorNode().getOperator().equals(MostPriorityOperator))) {
            copy.rotateLeft();
            copy = (BinaryExpressionNode) copy.getLeft();
            if (copy == null || copy.getRight() instanceof LiteralNode) {
                break;
            }
        }
    }

    @Override
    public void accept(ASTNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getType() {
        // Si l'opérateur est un opérateur logique, le type est un booléen
        if (operator.getOperator().equals("AND") || operator.getOperator().equals("AND ELSE") || operator.getOperator().equals("or")) {
            return "boolean";
        }
        // Si l'opérateur est un opérateur arithmétique, le type est un entier
        if (operator.getOperator().equals("+") || operator.getOperator().equals("-") || operator.getOperator().equals("*") || operator.getOperator().equals("/")) {
            return "integer";
        }
        return "integer";
    }
}