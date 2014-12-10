package cz.muni.fi.distributed.checker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.distributed.graph.Graph;
import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.TreeColorSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Handles the process of formula verification over given sub graph.
 */
public class FormulaVerificator {

    private final Graph graph;
    private final TaskDispatcher dispatcher;
    private final int taskId;

    public FormulaVerificator(Graph graph, TaskDispatcher dispatcher, int taskId) {
        this.graph = graph;
        this.dispatcher = dispatcher;
        this.taskId = taskId;
    }

    public void processFormula(@NotNull Formula formula) {
        if (formula.getOperator() == UnaryOperator.EXISTS_NEXT) {
            processNext(formula);
        } else if (formula.getOperator() == BinaryOperator.EXISTS_UNTIL) {
            processUntil(formula);
        } else if (formula.getOperator() == UnaryOperator.NEGATION) {
            //Map<Node, ColorSet> map = graph.factory.getAllValidNodes(formula.getSubFormulaAt(0));
            for (@NotNull Map.Entry<Integer, Node> node : graph.factory.nodeCache.entrySet()) {
                @NotNull ColorSet full = graph.getFullParamRange();
                full.subtract(node.getValue().getValidColors(formula.getSubFormulaAt(0)));
                if (!full.isEmpty()) {
                    Log.d("Negation, add: "+formula);
                    node.getValue().addFormula(formula, full);
                }
            }
            /*for (Map.Entry<Node, ColorSet> entry : map.entrySet()) {
                ColorSet full = graph.getFullParamRange();
                full.subtract(entry.getValue());
                if (!full.isEmpty()) {
                    Log.d("Negation, add: "+formula);
                    entry.getKey().addFormula(formula, full);
                }
            }*/
        } else if (formula.getOperator() == BinaryOperator.AND) {
            for (@NotNull Map.Entry<Integer, Node> node : graph.factory.nodeCache.entrySet()) {
                ColorSet full = node.getValue().getValidColors(formula.getSubFormulaAt(0));
                full.intersect(node.getValue().getValidColors(formula.getSubFormulaAt(1)));
                if (!full.isEmpty()) {
                    Log.d("ADD, add: "+formula);
                    node.getValue().addFormula(formula, full);
                }
            }
        }
    }

    public void processFormula(@NotNull Formula formula, @NotNull Node my, @NotNull ColorSet colorSet) {
        if (formula.getOperator() == UnaryOperator.EXISTS_NEXT) {
            //just add this and ignore rest - if everything is correct,
            //this should be called only with colorSets that have been
            // correctly reduced and send to us by other nodes
            my.addFormula(formula, colorSet);
        } else if (formula.getOperator() == BinaryOperator.EXISTS_UNTIL) {
            //intersect received colors with my colors in node,
            //if this is not empty and there are new colors, run back
            colorSet.intersect(my.getValidColors(formula.getSubFormulaAt(0)));
            if (!colorSet.isEmpty() && !my.getValidColors(formula).encloses(colorSet)) {
                my.addFormula(formula, colorSet);
                processUntilRecursive(my, formula, colorSet);
            }
        }
    }

    private void processNext(@NotNull Formula formula) {
        Formula sub = formula.getSubFormulaAt(0);
        Log.d("Process NEXT "+sub);
        @NotNull Map<Node, ColorSet> targets = graph.factory.getAllValidNodes(sub);
     //   Log.d("Found init nodes: "+targets.size());
        for (@NotNull Map.Entry<Node, ColorSet> source : targets.entrySet()) {
            @NotNull Map<Node, ColorSet> predecessors = graph.factory.computePredecessors(source.getKey(), source.getValue());
        //    Log.d("Predecessors found: "+predecessors.size());
            for (@NotNull Map.Entry<Node, ColorSet> predecessor : predecessors.entrySet()) {
                if (graph.isMyNode(predecessor.getKey())) {
                    //no need to intersect with source, because of how factory works
            //        Log.d("Add formula to predecessor.");
                    predecessor.getKey().addFormula(formula, predecessor.getValue());
                } else {
             //       Log.d("Dispatch task.");
                    dispatcher.dispatchNewTask(taskId, graph.partitioner.getGraphId(predecessor.getKey()), source.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }
        }
    }

    private void processUntil(@NotNull Formula formula) {
        @NotNull Map<Node, ColorSet> initial = graph.factory.getAllValidNodes(formula.getSubFormulaAt(1));
        Log.d("Process UNTIL "+formula.getSubFormulaAt(1)+" init nodes: "+initial.size());
        for (@NotNull Map.Entry<Node, ColorSet> init : initial.entrySet()) {
            //follow all colors valid for second formula
            if (!init.getValue().isEmpty() && !init.getKey().getValidColors(formula).encloses(init.getValue())) {
                init.getKey().addFormula(formula, init.getValue());
                processUntilRecursive(init.getKey(), formula, init.getValue());
            }
        }
    }


    private void processUntilRecursive(@NotNull Node node, @NotNull Formula formula, ColorSet colorSet) {
        //follow all colors that got us to this node
        @NotNull Map<Node, ColorSet> predecessors = graph.factory.computePredecessors(node, colorSet);
        //Log.d("Predecessors found: "+predecessors.size());
        for (@NotNull Map.Entry<Node, ColorSet> path : predecessors.entrySet()) {
            if (graph.isMyNode(path.getKey())) {
                //intersect transition colors with valid colors for first formula in predecessor
                ColorSet set = path.getValue();
                set.intersect(path.getKey().getValidColors(formula.getSubFormulaAt(0)));
                //if result isn't empty and if any new colors are introduced
                if (!set.isEmpty() && !path.getKey().getValidColors(formula).encloses(set)) {
                    Log.d("Add Until: "+set+" "+path.getValue());
                    path.getKey().addFormula(formula, set);
                    processUntilRecursive(path.getKey(), formula, path.getValue());
                }
            } else {
                dispatcher.dispatchNewTask(taskId, graph.partitioner.getGraphId(path.getKey()), node, path.getKey(), path.getValue());
            }
        }
    }

}
