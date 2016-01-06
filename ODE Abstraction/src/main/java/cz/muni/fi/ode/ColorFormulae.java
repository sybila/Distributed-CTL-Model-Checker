package cz.muni.fi.ode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


/**
 * Created by User on 7.12.15.
 */
public class ColorFormulae implements ColorSet {

    private final Context ctx;
    private final Solver solver;

    private BoolExpr formula;
    private Boolean sat = null;

    public ColorFormulae(Context c, Solver s) {
        this.ctx = c;
        this.solver = s;
        this.formula = ctx.mkTrue();
        this.sat = true;
    }

    public ColorFormulae(Context c, Solver s, BoolExpr formula) {
        this.ctx = c;
        this.solver = s;
        this.formula = formula;
        this.sat = true;
    }

    public ColorFormulae(ColorFormulae f) {
        this.ctx = f.ctx;
        this.solver = f.solver;
        this.formula = f.formula;
        this.sat = f.sat;
    }

    public ColorFormulae() {
        this.ctx = new Context();
        this.solver = ctx.mkSolver();
        this.formula = ctx.mkTrue();
        this.sat = true;
    }

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
                solver.add(formula);
                //System.out.println("Is sat? "+solver.check());
                boolean sat = solver.check() == Status.SATISFIABLE;
                this.sat = sat;
                solver.reset();
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

    public static ColorSet createFromBuffer(@NotNull int[] lengths, double[] data) {
        throw new NotImplementedException();
    }

    public static ColorSet createCopy(@NotNull ColorSet set) {
        return new ColorFormulae((ColorFormulae) set);
    }
}
