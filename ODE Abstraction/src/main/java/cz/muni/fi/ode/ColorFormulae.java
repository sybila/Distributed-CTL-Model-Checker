package cz.muni.fi.ode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;

/**
 * Created by User on 7.12.15.
 */
public class ColorFormulae implements ColorSet {

    private Solver formulae;

    public ColorFormulae(int initialCapacity) { Context ctx = new Context(); this.formulae = ctx.mkSolver(); }

    public ColorFormulae() { Context ctx = new Context(); this.formulae = ctx.mkSolver(); }

//    public ColorFormulae(@NotNull Collection<? extends RangeSet<Double>> c) {}

    public BoolExpr[] getAssertions() {
        return (this.formulae.getAssertions());
    }

    @Override
    public void intersect(@NotNull ColorSet set) {
        // the most naive version
        this.formulae.add(((ColorFormulae)set).getAssertions());
    }

    @Override
    public void subtract(@NotNull ColorSet set) {

    }

    @Override
    public boolean union(@NotNull ColorSet set) {
        BoolExpr[] expr = this.formulae.getAssertions();
        Context ctx = new Context();
        //this.formulae = ctx.mkSolver(ctx.mkOr(expr,((ColorFormulae)set).getAssertions()));
        return false;
    }

    @Override
    public boolean isEmpty() {
        return(this.formulae.check() != Status.SATISFIABLE);
    }
}
