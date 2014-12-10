package cz.muni.fi;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.operator.BinaryOperator;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.distributed.checker.FormulaChecker;
import cz.muni.fi.distributed.graph.Graph;
import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.Model;
import cz.muni.fi.model.TreeColorSet;
import mpi.MPI;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

public class DistributedModelChecker {

    @NotNull
    public final Graph graph;

    public DistributedModelChecker(@NotNull Model model, int mpiSize, int mpiRank) {
        this.graph = new Graph(model, mpiSize, mpiRank);
    }

    public void check(Formula formula) throws InterruptedException {
        //propositions are trivially checked in all nodes
        if (formula instanceof Proposition) {
            @NotNull Map<Node, ColorSet> valid = graph.factory.getAllValidNodes(formula);
            Log.d(formula+" Proposition nodes found: "+valid.size());
            for (@NotNull Map.Entry<Node, ColorSet> entry : valid.entrySet()) {
                entry.getKey().addFormula(formula, entry.getValue());
            }
            return;
        }
        //recursively check all sub formulas
        for (Formula sub : formula.getSubFormulas()) {
            check(sub);
        }
        Log.d("--------------- Checking: "+formula+" ------------------");
        //create new task to process this formula and wait for it to finish
        @NotNull FormulaChecker checker = new FormulaChecker(formula, graph, MPI.COMM_WORLD);
        checker.execute();
    }

}
