package cz.muni.fi;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.ParameterSet;
import cz.muni.fi.graph.Path;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class ModelChecker {

    public static void initialize(int numberOfParameters) {
        ParameterSet.limit = numberOfParameters;
    }

    public static void check(Graph graph, Formula formula) {
        if (formula instanceof Proposition) {
            if (formula instanceof FloatProposition) {
                checkProposition(graph, (FloatProposition) formula);
            }
            return;
        }
        for (Formula form : formula.getSubFormulas()) {     //first check all subFormulas
            check(graph, form);
        }
        if (formula.getOperator() == BinaryOperator.AND) {
            checkAnd(graph, formula);
        }
        if (formula.getOperator() == UnaryOperator.NEGATION) {
            checkNegation(graph, formula);
        }
        if (formula.getOperator() == UnaryOperator.EXISTS_NEXT) {
            checkExistsNext(graph, formula);
        }
        if (formula.getOperator() == UnaryOperator.ALL_FUTURE) {
            checkAllFuture(graph, formula);
        }
        if (formula.getOperator() == BinaryOperator.EXISTS_UNTIL) {
            checkExistsUntil(graph, formula);
        }
    }

    private static void checkProposition(Graph graph, FloatProposition proposition) {
        Queue<Node> queue = new LinkedList<>();
        Set<Node> found = new HashSet<>();
        queue.addAll(graph.getInitialNodes());
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            found.add(node);
            if (proposition.evaluate(node.getVariable(proposition.getVariable()))) {
                node.addFormula(proposition, graph.getColors());
            }
            for (Path path : node.getAfter()) {
                if (!found.contains(path.getTo())) {
                    queue.add(path.getTo());
                }
            }
        }
    }

    private static void checkExistsUntilNode(Graph graph, Node node, Formula formula, Set<Node> found) {
        found.add(node);

        //get all colors where second sub formula is true
        Set<Integer> valid = node.getValidColors(formula.getSubFormulaAt(1));
        if (!valid.isEmpty()) {
            node.addFormula(formula, valid);
        }

        //check recursively all future nodes
        for (Path path : node.getAfter()) {
            if (!found.contains(path.getTo())) {
                checkExistsUntilNode(graph, path.getTo(), formula, found);
            }
        }

        //check for what colors some of them hold
        valid = new HashSet<>();
        for (Path path : node.getAfter()) {
            valid.addAll(path.getTo().getValidColors(formula));
        }
        //intersect colors that hold for first sub formula
        valid.retainAll(node.getValidColors(formula.getSubFormulaAt(0)));

        //add colors
        if (!valid.isEmpty()) {
            node.addFormula(formula, valid);
        }
    }

    private static void checkExistsUntil(Graph graph, Formula formula) {
        Set<Node> found = new HashSet<>();
        for (Node node : graph.getInitialNodes()) {
            checkExistsUntilNode(graph, node, formula, found);
        }
    }

    private static void checkAllFutureNode(Graph graph, Node node, Formula formula, Set<Node> found) {
        found.add(node);

        //get all colors where sub formula is true
        Set<Integer> valid = node.getValidColors(formula.getSubFormulaAt(0));
        if (!valid.isEmpty()) {
            node.addFormula(formula, valid);
        }

        //check recursively all future nodes
        for (Path path : node.getAfter()) {
            if (!found.contains(path.getTo())) {
                checkAllFutureNode(graph, path.getTo(), formula, found);
            }
        }

        //check for what colors all of them hold
        valid = graph.getColorsCopy();
        for (Path path : node.getAfter()) {
            valid.retainAll(path.getTo().getValidColors(formula));
        }
        //add colors
        if (!valid.isEmpty()) {
            node.addFormula(formula, valid);
        }
    }

    private static void checkAllFuture(Graph graph, Formula formula) {
        Set<Node> found = new HashSet<>();
        for (Node node : graph.getInitialNodes()) {
            checkAllFutureNode(graph, node, formula, found);
        }
    }

    private static void checkExistsNext(Graph graph, Formula formula) {
        Queue<Node> queue = new LinkedList<>();
        HashSet<Node> found = new HashSet<>();
        queue.addAll(graph.getInitialNodes());
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            found.add(node);
            Set<Integer> exists = new HashSet<>();
            for (Path path : node.getAfter()) {
                exists.addAll(path.getTo().getValidColors(formula.getSubFormulaAt(0)));
            }
            if (!exists.isEmpty()) {
                node.addFormula(formula, exists);
            }
            for (Path path : node.getAfter()) {
                if (!found.contains(path.getTo())) {
                    queue.add(path.getTo());
                }
            }
        }
    }

    private static void checkAnd(Graph graph, Formula formula) {
        Queue<Node> queue = new LinkedList<>();
        HashSet<Node> found = new HashSet<>();
        queue.addAll(graph.getInitialNodes());
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            found.add(node);
            Set<Integer> valid = node.getValidColors(formula.getSubFormulaAt(0));
            valid.retainAll(node.getValidColors(formula.getSubFormulaAt(1)));
            if (!valid.isEmpty()) {
                node.addFormula(formula, valid);
            }
            for (Path path : node.getAfter()) {
                if (!found.contains(path.getTo())) {
                    queue.add(path.getTo());
                }
            }
        }
    }

    private static void checkNegation(Graph graph, Formula formula) {
        Queue<Node> queue = new LinkedList<>();
        HashSet<Node> found = new HashSet<>();
        queue.addAll(graph.getInitialNodes());
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            found.add(node);
            Set<Integer> valid = node.getValidColors(formula.getSubFormulaAt(0));
            Set<Integer> all = graph.getColorsCopy();
            all.removeAll(valid);
            if (!all.isEmpty()) {
                node.addFormula(formula, all);
            }
            for (Path path : node.getAfter()) {
                if (!found.contains(path.getTo())) {
                    queue.add(path.getTo());
                }
            }
        }
    }
}
