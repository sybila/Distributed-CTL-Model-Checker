package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.Operator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;

/**
 * Creates new verificator for given formula.
 */
public class FormulaVerificator<N extends Node, C extends ColorSet> {

    private final StateSpacePartitioner<N> partitioner;
    private final ModelAdapter<N, C> model;
    private final TaskMessenger<N, C> taskMessenger;
    private final Terminator.TerminatorFactory terminatorFactory;


    public FormulaVerificator(ModelAdapter<N, C> model, StateSpacePartitioner<N> partitioner, TaskMessenger<N, C> taskMessenger, Terminator.TerminatorFactory terminatorFactory) {
        this.partitioner = partitioner;
        this.model = model;
        this.taskMessenger = taskMessenger;
        this.terminatorFactory = terminatorFactory;
    }

    public void verifyFormula(Formula formula) {
        Operator operator = formula.getOperator();
        FormulaProcessor processor;
        if (operator == UnaryOperator.NEGATION) {
            processor = new NegationVerificator<>(model, formula);
        } else if(operator == BinaryOperator.AND) {
            processor = new AndVerificator<>(model, formula);
        } else if(operator == BinaryOperator.OR) {
            processor = new OrVerificator<>(model, formula);
        } else if(operator == BinaryOperator.EXISTS_UNTIL) {
            processor = new ExistsUntilVerificator<>(model, partitioner, formula, terminatorFactory, taskMessenger);
        } else if(operator == BinaryOperator.ALL_UNTIL) {
            processor = new AllUntilVerificator<>(model, partitioner, formula, terminatorFactory, taskMessenger);
        } else if(operator == UnaryOperator.EXISTS_NEXT) {
            processor = new NextVerificator<>(model, partitioner, formula, terminatorFactory, taskMessenger);
        } else {
            throw new IllegalArgumentException("Cannot verify operator: "+operator);
        }
        processor.verify();
    }
}
