package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.Operator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;

/**
 * Creates new verificator for given formula.
 */
public class FormulaVerificatorFactory<N extends Node, C extends ColorSet> {

    private final StateSpacePartitioner<N> partitioner;
    private final ModelAdapter<N, C> model;
    private final int myId;


    public FormulaVerificatorFactory(StateSpacePartitioner<N> partitioner, ModelAdapter<N, C> model, int myId) {
        this.partitioner = partitioner;
        this.model = model;
        this.myId = myId;
    }

    public FormulaVerificator<N,C> getVerificatorForFormula(Formula formula) {
        Operator operator = formula.getOperator();
        if (operator == UnaryOperator.NEGATION) {
            return new NegationVerificator<>(myId, model, partitioner, formula);
        } else if(operator == BinaryOperator.AND) {
            return new AndVerificator<>(myId, model, partitioner, formula);
        } else if(operator == BinaryOperator.OR) {
            return new OrVerificator<>(myId, model, partitioner, formula);
        } else if(operator == BinaryOperator.EXISTS_UNTIL) {
            return new ExistsUntilVerificator<>(myId, model, partitioner, formula);
        } else if(operator == BinaryOperator.ALL_UNTIL) {
            return new AllUntilVerificator<>(myId, model, partitioner, formula);
        } else if(operator == UnaryOperator.EXISTS_NEXT) {
            return new NextVerificator<>(myId, model, partitioner, formula);
        } else {
            throw new IllegalArgumentException("Cannot verify operator: "+operator);
        }
    }
}
