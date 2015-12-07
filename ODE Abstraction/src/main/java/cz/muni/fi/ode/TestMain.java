package cz.muni.fi.ode;

import com.microsoft.z3.*;

/**
 * Created by User on 6.12.15.
 */
public class TestMain {

    public static void main(String[] args) {
        System.out.println("TestMain for Z3");

        int paramCount = 3;
        String[] paramNames = new String[] {"p1","p2","p3"};
        Double[] paramConsts = new Double[] {1.5, -0.02, 0.0};

        Context ctx = new Context();

        final RealExpr zero = ctx.mkReal("0");
        RealExpr[] params = new RealExpr[paramCount];
        RealExpr[] consts = new RealExpr[paramCount];
        for(int i = 0; i < paramCount; i++) {
            params[i] = ctx.mkRealConst(paramNames[i]);
            consts[i] = ctx.mkReal(paramConsts[i].toString());
        }

        Expr expr = ctx.mkMul(params[0],consts[0]);
        for(int i = 1; i < paramCount; i++) {
            expr = ctx.mkAdd((ArithExpr) expr, ctx.mkMul(params[i],consts[i]));
        }
        expr = ctx.mkLt(zero, (ArithExpr) expr);

//        RealExpr x = ctx.mkRealConst("x");
//        RealExpr y = ctx.mkRealConst("y");
//        RealExpr z = ctx.mkRealConst("z");
//        Double xC = -1.5;
//        RealExpr a = ctx.mkReal(xC.toString());

//        BoolExpr expr = ctx.parseSMTLIB2String("x > y + 3");
//        Expr expr = ctx.mkLt(x, ctx.mkSub(a, ctx.mkAdd(x, y))); // x < a - (x + y)

        System.out.println(expr);
        System.out.println(expr.simplify());


        Solver s = ctx.mkSolver();

        s.add(ctx.mkLt(params[0],params[1]));
        s.add((BoolExpr)expr);
        //s.add(ctx.mkAnd((BoolExpr) expr,ctx.mkLt(params[0],params[1])));

        if(s.check() == Status.SATISFIABLE) {
            System.out.println("Sat");
            System.out.println(s.getModel());
        } else
            System.out.println("Unsat");
/*
        System.out.println("Arguments:");
        for(Expr ex : s.getAssertions()) {
            for (Expr e : ex.getArgs()) System.out.println(e);
            System.out.println("---");
        }
*/
    }
}
