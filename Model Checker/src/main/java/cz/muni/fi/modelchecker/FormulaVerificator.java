package cz.muni.fi.modelchecker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.TaskManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Class used for model verification.
 */
public class FormulaVerificator<N extends Node, C extends ColorSet> {

    private final ModelAdapter<N, C> model;
    private TaskManager<N, C> taskManager;
    private final StateSpacePartitioner<N> partitioner;
    private final int myId;

    public FormulaVerificator(int myId, ModelAdapter<N, C> model, StateSpacePartitioner<N> partitioner) {
        this.model = model;
        this.partitioner = partitioner;
        this.myId = myId;
    }

    public void setTaskManager(TaskManager<N, C> taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * Process formula without any extra data.
     * (this is usually executed from root task on each network node)
     */
    public void processFormula(@NotNull Formula formula) {
        if (formula.getOperator() == UnaryOperator.EXISTS_NEXT) {
            processNext(formula);
        } else if (formula.getOperator() == BinaryOperator.EXISTS_UNTIL) {
            processUntil(formula);
        } else if (formula.getOperator() == UnaryOperator.NEGATION) {
            Map<N, C> nodes = model.initialNodes(formula.getSubFormulaAt(0));
            Map<N, C> inversion = model.invertNodeSet(nodes);
            for (Map.Entry<N, C> entry : inversion.entrySet()) {
                model.addFormula(entry.getKey(), formula, entry.getValue());
            }
        } else if (formula.getOperator() == BinaryOperator.AND) {
            Map<N, C> first = model.initialNodes(formula.getSubFormulaAt(0));
            Map<N, C> second = model.initialNodes(formula.getSubFormulaAt(1));
            //intersect node sets
            for (Map.Entry<N, C> entry : first.entrySet()) {
                C colorSet = second.get(entry.getKey());
                if (colorSet != null) {
                    colorSet.intersect(entry.getValue());
                    model.addFormula(entry.getKey(), formula, colorSet);
                }
            }
        } else if (formula.getOperator() == BinaryOperator.OR) {
            for (Map.Entry<N, C> entry : model.initialNodes(formula.getSubFormulaAt(0)).entrySet()) {
                model.addFormula(entry.getKey(), formula, entry.getValue());
            }
            for (Map.Entry<N, C> entry : model.initialNodes(formula.getSubFormulaAt(1)).entrySet()) {
                model.addFormula(entry.getKey(), formula, entry.getValue());
            }
        }
    }

    /**
     * Process formula, but start from given node and parameters.
     * (usually executed by secondary task requested from another node)
     */
    public void processFormula(@NotNull Formula formula, @NotNull N my, @NotNull C colorSet) {
        if (formula.getOperator() == UnaryOperator.EXISTS_NEXT) {
            //just add this and ignore rest - if everything is correct,
            //this should be called only with colorSets that have been
            //correctly reduced and send to us by other nodes
            model.addFormula(my, formula, colorSet);
        } else if (formula.getOperator() == BinaryOperator.EXISTS_UNTIL) {
            //intersect received colors with my colors in node,
            //if this is not empty and there are new colors, run back
            colorSet.intersect(model.validColorsFor(my, formula.getSubFormulaAt(0)));
            if (model.addFormula(my, formula, colorSet)) {
                processUntilRecursive(my, formula, colorSet);
            }
        }
    }

    private void processNext(@NotNull Formula formula) {
        //find all nodes that match sub formula
        for (Map.Entry<N, C> initial : model.initialNodes(formula.getSubFormulaAt(0)).entrySet()) {
            //for each node, find it's predecessors
            for (Map.Entry<N, C> predecessor : model.predecessorsFor(initial.getKey(), initial.getValue()).entrySet()) {
                //no need to intersect colors, since predecessorsFor respects color bounds
                int owner = partitioner.getNodeOwner(predecessor.getKey());
                if (owner == myId) {
                    model.addFormula(predecessor.getKey(), formula, predecessor.getValue());
                } else {
                    taskManager.dispatchTask(owner, initial.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }
        }
    }

    private void processUntil(@NotNull Formula formula) {
        model.initialNodes(formula.getSubFormulaAt(1)).entrySet().stream()
                .filter //add formula to all initial nodes
                        (initial -> model.addFormula(initial.getKey(), formula, initial.getValue()))
                .forEach //process recursively only nodes that have been modified by previous change
                        (initial -> processUntilRecursive(initial.getKey(), formula, initial.getValue()));
    }


    private void processUntilRecursive(@NotNull N node, @NotNull Formula formula, C colorSet) {
        //examine all predecessors
        for (Map.Entry<N, C> predecessor : model.predecessorsFor(node, colorSet).entrySet()) {
            int owner = partitioner.getNodeOwner(predecessor.getKey());
            if (myId == owner) {
                //if predecessor is mine, intersect colors where sub formula 0 holds and add them
                //if addition has changed anything, proceed evaluation with reduced colors
                C colors = predecessor.getValue();
                colors.intersect(model.validColorsFor(predecessor.getKey(), formula.getSubFormulaAt(0)));
                if (model.addFormula(predecessor.getKey(), formula, colors)) {
                    processUntilRecursive(predecessor.getKey(), formula, colors);
                }
            } else {
                taskManager.dispatchTask(owner, node, predecessor.getKey(), predecessor.getValue());
            }
        }
    }

}
