package cz.muni.fi.checker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.FormulaImpl;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.ctl.formula.proposition.ValueEquals;
import cz.muni.fi.dot.DotParser;
import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.Path;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        Graph graph = DotParser.parse(new File("/Users/daemontus/Downloads/tcbb1.bio.prop1.bio.dot"));
        Formula f = new ValueEquals(0, 5);
        //Formula l = new FormulaImpl(UnaryOperator.NEGATION, f);
        Formula k = new FormulaImpl(UnaryOperator.NEGATION, f);
        List<Formula> list = new ArrayList<Formula>();
        list.add(new ValueEquals(1, 1));
        list.add(k);
        //ModelChecker.check(graph, new FormulaImpl(BinaryOperator.AND, list));
        ModelChecker.check(graph, new FormulaImpl(UnaryOperator.ALL_FUTURE, new ValueEquals(1, 10)));
        //ModelChecker.check(graph, k);
        printRecursive(graph.getInitialNode(), 5);
    }


    private static void printRecursive(Node node, int k) {
        if (k<=0) return;
        System.out.println(node.toString());
        System.out.println(node.correct.size()+" "+Arrays.toString(node.correct.keySet().toArray()));
        for (Path path : node.getAfter()) {
            Node n = path.getTo();
            printRecursive(n, k-1);
        }
    }

}
