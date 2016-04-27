package cz.muni.fi.ode;

import com.microsoft.z3.*;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


public class ColorFormulae implements ColorSet {

    public static long timeSpentInSolver = 0;
    public static long solverUsedCount = 0;

    private final Context ctx;
    private final Solver solver;

    //private static Map<BoolExpr, BoolExpr> solverCache = new HashMap<>();
    //public static int cacheHit = 0;

    private BoolExpr formula;
    private Boolean sat = null;
    private Goal goal = null;
    private Tactic tactic = null;
/*
    public ColorFormulae(Context c, Solver s) {
        this.ctx = c;
        this.solver = s;
        this.formula = ctx.mkTrue();
        this.sat = true;
        tactic = ctx.mkTactic("ctx-solver-simplify");
    }*/

    public ColorFormulae(Context c, Solver s, Goal g, Tactic t, BoolExpr formula) {
        this.ctx = c;
        this.solver = s;
        this.formula = formula;
        this.goal = g;
        this.sat = null;
        tactic = t;
    }

    public ColorFormulae(ColorFormulae f) {
        this.ctx = f.ctx;
        this.solver = f.solver;
        this.formula = f.formula;
        this.goal = f.goal;
        this.sat = f.sat;
        this.tactic = f.tactic;
    }
/*
    public ColorFormulae() {
        this.ctx = new Context();
        this.solver = ctx.mkSolver();
        this.formula = ctx.mkTrue();
        this.goal = ctx.mkGoal(false, false, false);
        this.sat = true;
        tactic = ctx.mkTactic("ctx-solver-simplify");
    }*/

    public Context getContext() {
        return ctx;
    }

    @Override
    public void intersect(@NotNull ColorSet set) {
        this.formula = ctx.mkAnd(formula, ((ColorFormulae) set).formula);
        sat = null;
    }

    public void intersect(BoolExpr expr) {
        this.formula = ctx.mkAnd(formula, expr);
        sat = null;
    }

    @Override
    public void union(@NotNull ColorSet set) {
        this.formula = ctx.mkOr(formula, ((ColorFormulae) set).formula);
    }

    @Override
    public boolean isEmpty() {
        synchronized (solver) {
            //System.out.println("SAT:"+sat);
            if (this.sat == null) {
                /*if (solverCache.containsKey(formula)) {
                    cacheHit += 1;
                    this.formula = solverCache.get(formula);
                    sat = !formula.isFalse();
                } else {*/
                    //BoolExpr orig = formula;
                    goal.add(formula);
                    long start = System.currentTimeMillis();
                    Goal[] result = tactic.apply(goal).getSubgoals();
                    timeSpentInSolver += System.currentTimeMillis() - start;
                    solverUsedCount += 1;
                    sat = !result[0].AsBoolExpr().isFalse();
                    BoolExpr[] exprs = new BoolExpr[result.length];
                    for (int i=0; i<result.length; i++) {
                        exprs[i] = result[i].AsBoolExpr();
                    }
                    formula = ctx.mkAnd(exprs);
                    //solverCache.put(orig, formula);
                    goal.reset();
                //}
                return !sat;
            } else return !sat;
        }
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    @NotNull
    @Override
    public String toString() {
        return(this.formula.toString());
    }

    @Override
    public void subtract(@NotNull ColorSet set) {
        this.formula = ctx.mkAnd(formula, ctx.mkNot(((ColorFormulae) set).formula));
        this.sat = null;
    }

    public boolean encloses(@NotNull ColorSet set) {
        BoolExpr subtract = ctx.mkAnd(((ColorFormulae) set).formula, ctx.mkNot(formula));//ctx.mkAnd(formula, ctx.mkNot(((ColorFormulae) set).formula));
        synchronized (solver) {
            solver.add(subtract);
           // System.out.println("Encloses"+subtract);
            boolean sat = solver.check() == Status.SATISFIABLE;
            solver.reset();
            return !sat;
        }
    }

    public static ColorSet createCopy(@NotNull ColorSet set) {
        return new ColorFormulae((ColorFormulae) set);
    }
}
