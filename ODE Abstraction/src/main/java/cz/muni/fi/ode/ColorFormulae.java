package cz.muni.fi.ode;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.microsoft.z3.*;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Created by User on 7.12.15.
 */
public class ColorFormulae implements ColorSet {

    final static String SAT = "Sat";
    final static String UNSAT = "Unsat";
    final static String UNDEF = "Undef";

    private Context ctx;
    private Solver formulae;

    public ColorFormulae(int initialCapacity) {
        this.ctx = new Context();
        this.formulae = ctx.mkSolver();
        //this.formulae.add(ctx.mkTrue());
    }

    public ColorFormulae(Context c) {
        this.ctx = c;
        this.formulae = ctx.mkSolver();
        //this.formulae.add(ctx.mkTrue());
    }

    public ColorFormulae() {
        this.ctx = new Context();
        this.formulae = ctx.mkSolver();
        //formulae.add(ctx.mkTrue());
    }

//    public ColorFormulae(@NotNull Collection<? extends RangeSet<Double>> c) {}

    public BoolExpr[] getAssertions() {
        return (this.formulae.getAssertions());
    }

    public Context getContext() {
        return(this.ctx);
    }

    public void addAssertion(Expr ex) {
        this.formulae.add((BoolExpr) ex.simplify());
    }

    public void addAssertions(BoolExpr[] ex) {
        this.formulae.add(ex);
    }

    @Override
    public void intersect(@NotNull ColorSet set) {
        // the most naive version
        this.formulae.add(((ColorFormulae)set).getAssertions());
    }

    @Override
    public boolean union(@NotNull ColorSet set) {
/*
        BoolExpr[] expr = this.formulae.getAssertions();
        Expr ex = this.ctx.mkTrue();
        for(Expr e : expr) {
            ex = this.ctx.mkAnd((BoolExpr) e.simplify(),(BoolExpr) ex).simplify();
        }

        ColorFormulae cf = (ColorFormulae) set;
        Context con = cf.getContext();
        BoolExpr[] expr2 = cf.getAssertions();
        Expr ex2 = con.mkTrue();
        for(Expr e2 : expr2) {
            ex2 = con.mkAnd((BoolExpr) e2.simplify(),(BoolExpr) ex2).simplify();
        }

        this.formulae = this.ctx.mkSolver();
        this.formulae.add((BoolExpr) this.ctx.mkOr((BoolExpr) ex,(BoolExpr) ex2).simplify());
*/
        Solver s = this.ctx.mkSolver();
        s.add((BoolExpr) this.ctx.mkOr((BoolExpr) mkConjunction(this),(BoolExpr) mkConjunction((ColorFormulae) set)).simplify());
        this.formulae = s;

        return false;
    }

    @Override
    public boolean isEmpty() {
        return(this.formulae.check() != Status.SATISFIABLE);
    }

    public String check() {
        Status st = this.formulae.check();
        if(st == Status.SATISFIABLE) return (SAT);
        if(st == Status.UNSATISFIABLE) return (UNSAT);
        if(st == Status.UNKNOWN) return (UNDEF);
        return ("CHECK ERROR");
    }

    @NotNull
    @Override
    public String toString() {
        return(this.formulae.toString());
    }

    @Override
    public void subtract(@NotNull ColorSet set) {

        ColorFormulae cf = (ColorFormulae) set;
        Context con = cf.getContext();
        BoolExpr[] expr2 = cf.getAssertions();
        Expr ex2 = con.mkTrue();
        for(Expr e2 : expr2) {
            ex2 = con.mkAnd((BoolExpr) e2.simplify(),(BoolExpr) ex2).simplify();
        }
        ex2 = con.mkNot((BoolExpr) ex2).simplify();

//        Expr ex2 = this.ctx.mkNot((BoolExpr) mkConjunction((ColorFormulae) set)).simplify();
        this.formulae.add((BoolExpr) ex2);
    }

    public static Expr mkConjunction(ColorFormulae cf) {
        Context con = cf.getContext();
        BoolExpr[] expr2 = cf.getAssertions();
        Expr ex2 = con.mkTrue();
        for(Expr e2 : expr2) {
            ex2 = con.mkAnd((BoolExpr) e2.simplify(),(BoolExpr) ex2).simplify();
        }
        return (ex2);
    }

    public static Expr mkDisjunction(ColorFormulae cf) {
        Context con = cf.getContext();
        BoolExpr[] expr2 = cf.getAssertions();
        Expr ex2 = con.mkFalse();
        for(Expr e2 : expr2) {
            ex2 = con.mkOr((BoolExpr) e2.simplify(),(BoolExpr) ex2).simplify();
        }
        return (ex2);
    }

    // Not needed, always return false in order to satisfy compatibility
    public boolean encloses(@NotNull ColorSet set) {
        ColorFormulae cf = (ColorFormulae) ColorFormulae.createCopy(set);
        cf.subtract(this);
        return !cf.check().equals(SAT);
    }

    public static ColorSet createFromBuffer(@NotNull int[] lengths, double[] data) {
        return new ColorFormulae();
    }
/*
    @NotNull
    @SuppressWarnings("UnusedDeclaration")
    public Range<Double>[] asArrayForParam(int i) {
        RangeSet<Double> set = new Range
        Set<Range<Double>> data = set.asRanges();
        return data.toArray(new Range[data.size()]);
    }
*/
    // It would not be needed after changes in StateSpaceGenerator class
    public static ColorSet derivedColorSet(@NotNull ColorSet ps, int pIndex, double lpValue, double rpValue) {
        return new ColorFormulae();
    }
/*
    // should be replaced by  new function in OdeModel class caled getEmptyColorSet() because the same context is needed
    public static ColorSet createEmpty(int dimensions) {
        ColorFormulae newSet = new ColorFormulae();

        return newSet;
    }
*/
    public static ColorSet createCopy(@NotNull ColorSet set) {
        ColorFormulae newSet = new ColorFormulae(((ColorFormulae) set).getContext());
        newSet.addAssertions(((ColorFormulae) set).getAssertions());
        return (newSet);
    }
}
