package cz.muni.fi.ode;

import com.microsoft.z3.*;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by User on 7.12.15.
 */
public class ColorFormulae implements ColorSet {

    public static long timeSpentInSolver = 0;
    public static long solverUsedCount = 0;

    private static Map<BoolExpr, BoolExpr> formulaCache = new HashMap<>();

    private final Context ctx;
    private final Solver solver;

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
        //test that formula is CNF
        //System.out.println("Is ok? "+formula);
        if (!formula.isAnd() && !formula.isBool()) {
            throw new IllegalArgumentException("Formula must be in CNF: "+formula);
        }
        for (Expr e : formula.getArgs()) {
            if (!(e instanceof BoolExpr) && !e.isOr()) {
                throw new IllegalArgumentException("Clouse is not a disjunction: "+formula+" clause: "+e);
            }
            if (e.isOr()) {
                for (Expr e2: e.getArgs()) {
                    if (!(e2.isBool() || e2.isGT() || e2.isGE() || e2.isLT() || e2.isLE())) {
                        throw new IllegalArgumentException("Literal is not a boolean formula: "+formula+" literal: "+e2);
                    }
                }
            }
        }
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

    public ColorFormulae copy(BoolExpr formula) {
        return new ColorFormulae(ctx, solver, goal, tactic, formula);
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

    /*public void intersect(BoolExpr expr) {
        this.formula = ctx.mkAnd(formula, expr);
        sat = null;
    }*/

    @Override
    public void union(@NotNull ColorSet set) {
      //  System.out.println("Before union: "+formula+" "+set);
        this.sat = null;    //force simplification when this is done
        this.formula = union(this.formula, ((ColorFormulae) set).formula);
      //  System.out.println("After union: "+formula);
    }

    private BoolExpr union(BoolExpr one, BoolExpr two) {
        if (one.isTrue() || two.isFalse()) return one; // x or FF = x, TT or x = TT
        if (one.isFalse() || two.isTrue()) {   // FF or x = x, x or TT = TT
            return two;
        }
        //now we know that formula is not bool, so it can be a conjunction of clauses or one disjunction of literals
        if (one.isOr() && two.isOr()) {  //both clauses
            return ctx.mkOr(one, two);
        }
        if (one.isOr() || two.isOr()) {
            BoolExpr clause = one.isOr() ? one : two;
            BoolExpr formula = one.isOr() ? two : one;

            BoolExpr[] newClauses = new BoolExpr[formula.getNumArgs()];
            Expr[] args = formula.getArgs();
            for (int i = 0; i < args.length; i++) {
                BoolExpr c = (BoolExpr) args[i];
                newClauses[i] = ctx.mkOr(clause, c);
            }
            return ctx.mkAnd(newClauses);
        }
      //  System.out.println("One: "+one.getNumArgs()+" "+Arrays.toString(one.getArgs())+" "+one.isAnd());
      //  System.out.println("Two: "+two.getNumArgs()+" "+Arrays.toString(two.getArgs())+" "+one.isAnd());

        BoolExpr[] newClauses = new BoolExpr[one.getNumArgs() * two.getNumArgs()];
        int c = 0;
        for (Expr c1 : one.getArgs()) {
            for (Expr c2 : two.getArgs()) {
                newClauses[c] = ctx.mkOr((BoolExpr) c1, (BoolExpr) c2);
                c += 1;
            }
        }
       // System.out.println("New clauses: "+ Arrays.toString(newClauses));
        return ctx.mkAnd(newClauses);
    }

    private void recursivePrint(Expr e) {
       // System.out.println(e+" --- and: "+e.isAnd()+" args: "+e.getNumArgs()+" "+Arrays.toString(e.getArgs()));
        for (Expr e1 : e.getArgs()) {
            recursivePrint(e1);
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (solver) {
            //System.out.println("SAT:"+sat);
            if (this.sat == null) {
                /*solver.add(formula);
                //System.out.println("Is sat? "+solver.check());
                boolean sat = solver.check() == Status.SATISFIABLE;
                this.sat = sat;
                solver.reset();
                return !sat;*/
                //Expr orig = formula;
                goal.add(formula);
              //  System.out.println("Before simplification: "+formula);
                long start = System.currentTimeMillis();
                Goal[] result = tactic.apply(goal).getSubgoals();
                timeSpentInSolver += System.currentTimeMillis() - start;
                solverUsedCount += 1;
                sat = !result[0].AsBoolExpr().isFalse();
                BoolExpr[] exprs = new BoolExpr[result.length];
                for (int i=0; i<result.length; i++) {
                    exprs[i] = result[i].AsBoolExpr();
                }
                formula = (BoolExpr) ctx.mkAnd(exprs).simplify();   //simplify removes extra and
              //  System.out.println("Simplified: "+formula);
                //Verify that we did the right thing:
                /*solver.add(formula);
                Status SAT = solver.check();
                System.out.println("SAT: "+ solver.check() + " sat: " + sat);
                if (SAT != Status.SATISFIABLE && sat) {
                    throw new IllegalStateException("Inconsistency! "+orig+" "+formula);
                }
                solver.reset();*/
                goal.reset();
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
        this.sat = null;
     //   System.out.println("Subtract: "+formula+" minus "+set);
        if (this.formula.isFalse()) return; // FF and !X = FF
        ColorFormulae other = (ColorFormulae) set;
        if (other.formula.isTrue()) {   // X and !TT = FF
            this.formula = ctx.mkFalse();
            return;
        }
        if (other.formula.isFalse()) return;    //X and !FF = X

        //first, make negation - negate each clause (get a CNF) and then union them
        BoolExpr negation = ctx.mkFalse();
        if (other.formula.isOr()) {
            negation = negatedClause(other.formula);
        } else {
            for (Expr e : other.formula.getArgs()) {
                negation = union(negation, negatedClause((BoolExpr) e));
            }
        }
      //  System.out.println("Negation: "+negation);
        this.formula = ctx.mkAnd(formula, negation);
      //  System.out.println("Result: "+formula);
    }

    private BoolExpr negatedClause(BoolExpr clause) {
        BoolExpr[] newExpr = new BoolExpr[clause.getNumArgs()];
        Expr[] args = clause.getArgs();
        for (int i = 0; i < args.length; i++) {
            BoolExpr e = (BoolExpr) args[i];
            newExpr[i] = ctx.mkNot(e);
        }
        return ctx.mkAnd(newExpr);
    }

    public boolean encloses(@NotNull ColorSet set) {
        BoolExpr subtract = ctx.mkAnd(((ColorFormulae) set).formula, ctx.mkNot(formula));
        synchronized (solver) {
            solver.add(subtract);
            // System.out.println("Encloses"+subtract);
            long start = System.currentTimeMillis();
            boolean sat = solver.check() == Status.SATISFIABLE;
           // timeSpentInSolver += System.currentTimeMillis() - start;
            //solverUsedCount += 1;
            solver.reset();
            return !sat;
        }
    }

    public static ColorSet createFromBuffer(@NotNull int[] lengths, double[] data) {
        throw new NotImplementedException();
    }

    public static ColorSet createCopy(@NotNull ColorSet set) {
        return new ColorFormulae((ColorFormulae) set);
    }


}
