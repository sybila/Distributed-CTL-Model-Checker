package cz.muni.fi.checker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.FormulaImpl;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.Operator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.Path;

import java.util.*;

/**
 * Created by daemontus on 23/09/14.
 */
public class ModelChecker {

    private static List<Integer> params = new ArrayList<Integer>();

    public static void check(Graph graph, Formula formula) {
        if (formula instanceof Proposition) {
            return;
        }
        for (Formula form : formula.getSubFormulas()) {     //first check all subFormulas
            check(graph, form);
        }
        if (formula.getOperator() == BinaryOperator.AND) {
            checkAnd(graph.getInitialNode(), formula);
        }
        if (formula.getOperator() == UnaryOperator.NEGATION) {
            checkNegation(graph.getInitialNode(), formula);
        }
        if (formula.getOperator() == UnaryOperator.EXISTS_NEXT) {
            checkExistsNext(graph.getInitialNode(), formula);
        }
        if (formula.getOperator() == UnaryOperator.ALL_FUTURE) {
            checkAllFuture(graph.getInitialNode(), formula);
        }
    }

    private static void checkAllFuture(Node node, Formula formula) {
        boolean change = true;
        while (change) {
            change = false;
            Queue<Node> queue = new LinkedList<Node>();
            HashSet<Node> found = new HashSet<Node>();
            queue.add(node);
            while (!queue.isEmpty()) {
                Node n = queue.poll();
                if (found.contains(n)) continue;
                found.add(n);
                boolean all = true;
                for (Path path : n.getAfter()) {
                    if (!path.getTo().correct.containsKey(formula)) {
                        all = false;
                        break;
                    }
                }
                if ((all || n.correct.containsKey(formula.getSubFormulas().get(0))) && !n.correct.containsKey(formula)) {
                    change = true;
                    n.correct.put(formula, params);
                }
                for (Path path : n.getAfter()) {
                    queue.add(path.getTo());
                }
            }
        }
    }

    private static void checkExistsNext(Node node, Formula formula) {
        boolean found = false;
        for (Path path : node.getAfter()) {
            if (path.getTo().correct.containsKey(formula.getSubFormulas().get(0))) {
                found = true;
            }
        }
        if (found) {
            node.correct.put(formula, params);
        }
        for (Path path : node.getBefore()) {
            checkExistsNext(path.getFrom(), formula);
        }
    }

    private static void checkAnd(Node node, Formula formula) {
        Queue<Node> queue = new LinkedList<Node>();
        HashSet<Node> found = new HashSet<Node>();
        queue.add(node);
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            if (found.contains(n)) continue;
            found.add(n);
            if (n.correct.containsKey(formula.getSubFormulas().get(0)) && n.correct.containsKey(formula.getSubFormulas().get(1))) {
                n.correct.put(formula, params);
            }
            for (Path path : n.getAfter()) {
                queue.add(path.getTo());
            }
        }
    }

    private static void checkNegation(Node node, Formula formula) {
        Queue<Node> queue = new LinkedList<Node>();
        HashSet<Node> found = new HashSet<Node>();
        queue.add(node);
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            if (found.contains(n)) continue;
            found.add(n);
            if (!n.correct.containsKey(formula.getSubFormulas().get(0))) {
                n.correct.put(formula, params);
            } else {
                n.correct.put(formula.getSubFormulas().get(0), params);
            }
            for (Path path : n.getAfter()) {
                queue.add(path.getTo());
            }
        }
    }
}
