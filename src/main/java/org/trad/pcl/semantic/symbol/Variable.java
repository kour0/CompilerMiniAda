package org.trad.pcl.semantic.symbol;

import com.diogonunes.jcolor.Attribute;
import org.trad.pcl.Helpers.TypeEnum;
import org.trad.pcl.Services.ErrorService;
import org.trad.pcl.semantic.SemanticAnalysisVisitor;

import static com.diogonunes.jcolor.Ansi.colorize;

public class Variable extends Symbol {

    protected String type;

    public Variable(String identifier, int shift) {
        super(identifier, shift);
        this.type = null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Variable { " +
                "identifier = '" + colorize(identifier, Attribute.YELLOW_TEXT()) + '\'' +
                ", type = '" + colorize(type, Attribute.YELLOW_TEXT()) + '\'' +
                ", shift = " + colorize(Integer.toString(shift), Attribute.RED_TEXT()) +
                " }";
    }
}
