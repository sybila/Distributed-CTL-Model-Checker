package cz.muni.fi;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.ctl.formula.proposition.Tautology;
import cz.muni.fi.distributed.graph.Graph;
import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.Model;
import cz.muni.fi.model.TreeColorSet;

import java.util.Set;

public class SequentialModelChecker {
/*
    private final Graph graph;

    public SequentialModelChecker(Model model) {
        graph = new Graph(model, 1, 0);
    }

    public void check(Formula formula) {
        if (formula instanceof Proposition) {
            //no need to calculate simple propositions - factory does that
            return;
        }
        for (Formula form : formula.getSubFormulas()) {     //first check all subFormulas
            check(form);
        }
        if (formula.getOperator() == BinaryOperator.AND) {
            checkAnd(formula);
        }
        if (formula.getOperator() == UnaryOperator.NEGATION) {
            checkNegation(formula);
        }
        if (formula.getOperator() == UnaryOperator.EXISTS_NEXT) {
            checkExistsNext(formula);
        }
        if (formula.getOperator() == BinaryOperator.EXISTS_UNTIL) {
            checkExistsUntil(formula);
        }
    }

    private void checkUntilRecursive(Node node, Formula formula) {
        //follow all colors that got us to this node
        Set<Node> predecessors = node.getPredecessors(formula.getSubFormulaAt(0));
        for (Node predecessor : predecessors) {
            //intersect transition colors with valid colors for first formula in predecessor
            ColorSet set = TreeColorSet.createCopy(predecessor.successors.get(node));
            set.intersect(predecessor.getValidColors(formula.getSubFormulaAt(0)));
            //if result isn't empty and if any new colors are introduced
            if (!set.isEmpty() && !predecessor.getValidColors(formula).encloses(set)) {
                predecessor.addFormula(formula, set);
                checkUntilRecursive(predecessor, formula);
            }
        }
    }

    private void checkExistsUntil(Formula formula) {
        //find all starting values
        Set<Node> initial = graph.factory.getAllValidNodes(formula.getSubFormulaAt(1));
        for (Node node : initial) {
            //follow all colors valid for second formula
            Set<Node> predecessors = node.getPredecessors(formula.getSubFormulaAt(1));
            for (Node predecessor : predecessors) {
                //intersect transition colors with valid colors for first formula in predecessor
                ColorSet set = TreeColorSet.createCopy(predecessor.successors.get(node));
                set.intersect(predecessor.getValidColors(formula.getSubFormulaAt(0)));
                //if result isn't empty and if any new colors are introduced
                if (!set.isEmpty() && !predecessor.getValidColors(formula).encloses(set)) {
                    predecessor.addFormula(formula, set);
                    checkUntilRecursive(predecessor, formula);
                }
            }
        }
    }

    private void checkExistsNext(Formula formula) {
        Formula sub = formula.getSubFormulaAt(0);
        Set<Node> targets = graph.factory.getAllValidNodes(sub);
        for (Node node : targets) {
            Set<Node> predecessors = node.getPredecessors(sub);
            for (Node predecessor : predecessors) {
                //every color that leads from node to it's predecessor
                ColorSet set = TreeColorSet.createCopy(predecessor.successors.get(node));
                predecessor.addFormula(formula, set);
            }
        }
    }

    private void checkAnd(Formula formula) {
        Set<Node> first = graph.factory.getAllValidNodes(formula.getSubFormulaAt(0));
        Set<Node> second = graph.factory.getAllValidNodes(formula.getSubFormulaAt(1));
        first.retainAll(second);
        for (Node node : first) {
            ColorSet set = TreeColorSet.createCopy(node.getValidColors(formula.getSubFormulaAt(0)));
            set.intersect(node.getValidColors(formula.getSubFormulaAt(1)));
            node.addFormula(formula, set);
        }
    }

    private void checkNegation(Formula formula) {
        Set<Node> all = graph.factory.getAllValidNodes(new Tautology());
        Set<Node> valid = graph.factory.getAllValidNodes(formula.getSubFormulaAt(0));
        all.removeAll(valid);
        for (Node node : all) {
            ColorSet set = graph.model.getFullColorSet();
            set.subtract(node.getValidColors(formula.getSubFormulaAt(0)));
            node.addFormula(formula, set);
        }
    }*/
}
