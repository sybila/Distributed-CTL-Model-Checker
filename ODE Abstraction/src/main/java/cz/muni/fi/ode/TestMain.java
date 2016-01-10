package cz.muni.fi.ode;

import com.microsoft.z3.*;

/**
 * Created by User on 6.12.15.
 */
public class TestMain {

    public static void main(String[] args) {

        Context ctx = new Context();

        ArithExpr x = ctx.mkRealConst("x");
        ArithExpr y = ctx.mkRealConst("y");

        ArithExpr z = ctx.mkReal("0");

        BoolExpr p1 = ctx.mkGt(z, ctx.mkAdd(ctx.mkReal("3.0"), x));
        BoolExpr p2 = ctx.mkLt(z, ctx.mkAdd(ctx.mkReal("3.0"), x));


        BoolExpr and = ctx.mkAnd(p1, p2);
        Expr sim = and.simplify();

        System.out.println(and + " " + sim);
        Tactic s = ctx.mkTactic("ctx-solver-simplify");

        Goal g = ctx.mkGoal(false, false, false);
        g.add(and);


        ApplyResult r = s.apply(g);
        System.out.println("false: "+r.getSubgoals()[0].AsBoolExpr().isFalse());
        System.out.println(r);
        //System.out.println(Arrays.toString(ctx.getTacticNames()));
        //System.out.println(s.getHelp());


        //System.out.println(System.getProperty("java.library.path"));
      /*  System.loadLibrary("ODE");

        //String filen = "ODE Abstraction/src/main/java/cz/muni/fi/ode/model2params.bio";
        String filen = "/Users/daemontus/Workspace/Sybila/Runtime/ODE/tcbb.bio";

        //read and prepare model
        @NotNull OdeModel model = new OdeModel(filen);
        model.load();

        @NotNull HashPartitioner partitioner = new HashPartitioner(model, 1, 0);
        @NotNull NodeFactory factory = new NodeFactory(model, partitioner);
        @NotNull StateSpaceGenerator generator = new StateSpaceGenerator(model, factory, partitioner.getMyLimit());
        factory.setGenerator(generator);

        Map<CoordinateNode, ColorFormulae> initial = factory.initialNodes(new FloatProposition(3.0, "E2F1", FloatProposition.Operator.GT_EQ));

        System.out.println("Initial: "+initial.size());
        for (Map.Entry<CoordinateNode, ColorFormulae> entry : initial.entrySet()) {
            System.out.println("E: "+entry.getKey()+" "+entry.getValue());
        }*/
/*
//-----------------------------------------------------------------------------------------------
        Map.Entry<CoordinateNode,ColorFormulae> initOne = initial.entrySet().iterator().next();
        //System.out.println("Context: "+initOne.getValue().getContext().toString());
        initial.clear();
        initial.put(initOne.getKey(),initOne.getValue());

        long start = System.currentTimeMillis();
        int count = 0;

        for(int i = 0; i < 100; i++) {
            Map<CoordinateNode,ColorFormulae> succ = new HashMap<>(initial);
            //initOne = (Map.Entry<CoordinateNode, ColorFormulae>) initial.entrySet().toArray()[initial.entrySet().toArray().length-1];
            //succ.put(initOne.getKey(),initOne.getValue());
            initial.clear();
            //initial = new HashMap<>();
            Map<CoordinateNode,ColorFormulae> newSucc;
            for(Map.Entry<CoordinateNode,ColorFormulae> entry : succ.entrySet()) {

                count++;
                //System.out.println("Node:\n"+entry.getKey().toString());
                //System.out.println("ColorSet:\n"+entry.getValue().toString());

                if(i == 0)
                    newSucc = factory.successorsFor(entry.getKey(), model.getFullColorSet());
                else
                    newSucc = factory.successorsFor(entry.getKey(), entry.getValue());

                for(Map.Entry<CoordinateNode,ColorFormulae> suc : newSucc.entrySet()) {
                    initial.put(suc.getKey(),suc.getValue());
                    //System.out.println("------------------------------");
                    //System.out.println("SuccNode:\n"+suc.getKey().toString());
                    //System.out.println("ColorSet:\n"+suc.getValue().toString()+"\nAssertions: "+suc.getValue().getAssertions().length);
                }
                System.out.println("======================================");
                System.out.println("Duration for "+count+" states: "+(System.currentTimeMillis() - start));
                System.out.println(i + " Map size: "+initial.size());
            }
            succ.clear();
            System.out.println("############################################");
        }
/*
        Map.Entry<CoordinateNode,ColorFormulae> initEntry = initial.entrySet().iterator().next();
        System.out.println("Node:\n"+initEntry.getKey().toString());
        System.out.println("ColorSet:\n"+initEntry.getValue().toString());

        Map<CoordinateNode, ColorFormulae> succ = factory.successorsFor(initEntry.getKey(), model.getFullColorSet());
        for(Map.Entry<CoordinateNode,ColorFormulae> entry2 : succ.entrySet()) {
            succ.put(entry2.getKey(),entry2.getValue());
            System.out.println("SuccNode:\n"+entry2.getKey().toString());
            System.out.println("ColorSet:\n"+entry2.getValue().toString());
            System.out.println("+++++++++++++++++++++++++++++++++++++");
        }
        System.out.println("---------------------------------------");
*/
        //System.out.println("Context: "+initial.entrySet().iterator().next().getValue().getContext().toString());
       // System.out.println("Final duration for "+count+" states: "+(System.currentTimeMillis() - start));

    }
/*
    public static void main2(String[] args) {
        System.out.println("TestMain for ColorFormulae class");

        int paramCount = 3;
        Context ctx = new Context();
        RealExpr zero = ctx.mkReal("0");
        RealExpr x = ctx.mkRealConst("x");
        RealExpr y = ctx.mkRealConst("y");
        RealExpr z = ctx.mkRealConst("z");

        Expr ex = ctx.mkAdd(x,y,z);
        Expr expr = ctx.mkLt(zero, (ArithExpr) ex).simplify();

        ColorFormulae cf = new ColorFormulae(ctx);
        cf.addAssertion(expr.simplify());
        System.out.println(cf);
        System.out.println(cf.check());

        Context ctx2 = new Context();
        RealExpr zero2 = ctx2.mkReal("0");
        RealExpr x2 = ctx2.mkRealConst("x");
        RealExpr y2 = ctx2.mkRealConst("y");
        RealExpr z2 = ctx2.mkRealConst("z");

        Expr ex2 = ctx2.mkAdd(x2,y2,z2);
        Expr expr2 = ctx2.mkLt(zero2, (ArithExpr) ex2).simplify();

        ColorFormulae cf2 = new ColorFormulae(cf.getContext());
        cf2.addAssertion(ctx.mkNot((BoolExpr) expr).simplify());
        System.out.println(cf2);
        System.out.println(cf2.check());

        ColorFormulae cfempty = new ColorFormulae(ctx);
        cfempty.addAssertion(ctx.mkFalse());

        cf.intersect(cf2);
        System.out.println(cf);
        System.out.println(cf.check());

        cf.union(cf2);
        System.out.println(cf);
        System.out.println(cf.check());

        cf.subtract(cf2);
        System.out.println(cf);
        System.out.println(cf.check());

        System.out.println(cfempty);
        System.out.println(cfempty.check());

        System.out.println(cf2);
        System.out.println(cf2.check());

        cf2.intersect(cfempty);
        System.out.println(cf2);
        System.out.println(cf2.check());
    }

    public static void main3(String[] args) {
        System.out.println("TestMain for Z3");

        int paramCount = 3;
        String[] paramNames = new String[] {"p1","p2","p3"};
        Double[] paramConsts = new Double[] {1.5, -0.02, 0.0, -4.3};

        Context ctx = new Context();

        final RealExpr zero = ctx.mkReal("0");
        RealExpr[] params = new RealExpr[paramCount];
        RealExpr[] consts = new RealExpr[paramCount+1];
        int i = 0;
        for( ; i < paramCount; i++) {
            params[i] = ctx.mkRealConst(paramNames[i]);
            consts[i] = ctx.mkReal(paramConsts[i].toString());
        }
        consts[i] = ctx.mkReal(paramConsts[i].toString());

        Expr expr = ctx.mkMul(params[0],consts[0]).simplify();
        for(i = 1; i < paramCount; i++) {
            expr = ctx.mkAdd((ArithExpr) expr,(ArithExpr) ctx.mkMul(params[i],consts[i]).simplify()).simplify();
        }
        expr = ctx.mkAdd((ArithExpr) expr, consts[i]).simplify();
        expr = ctx.mkLt(zero, (ArithExpr) expr).simplify();

//        RealExpr x = ctx.mkRealConst("x");
//        RealExpr y = ctx.mkRealConst("y");
//        RealExpr z = ctx.mkRealConst("z");
//        Double xC = -1.5;
//        RealExpr a = ctx.mkReal(xC.toString());

//        BoolExpr expr = ctx.parseSMTLIB2String("x > y + 3");
//        Expr expr = ctx.mkLt(x, ctx.mkSub(a, ctx.mkAdd(x, y))); // x < a - (x + y)

//        System.out.println(expr);

        ColorFormulae cf = new ColorFormulae(ctx);
        cf.addAssertion(expr);
        System.out.println(cf);
        System.out.println(cf.check());

        Double[] paramConsts2 = new Double[] {0.0, 0.02, -2.0, 0.0};
        RealExpr[] consts2 = new RealExpr[paramCount+1];
        i = 0;
        for( ; i < paramCount; i++) {
//            params[i] = ctx.mkRealConst(paramNames[i]);
            consts2[i] = ctx.mkReal(paramConsts2[i].toString());
        }
        consts2[i] = ctx.mkReal(paramConsts2[i].toString());

        expr = ctx.mkMul(params[0],consts2[0]).simplify();
        for(i = 1; i < paramCount; i++) {
            expr = ctx.mkAdd((ArithExpr) expr,(ArithExpr) ctx.mkMul(params[i],consts2[i]).simplify()).simplify();
        }
        expr = ctx.mkAdd((ArithExpr) expr, consts2[i]).simplify();
        expr = ctx.mkLt(zero, (ArithExpr) expr).simplify();

        ColorFormulae cf2 = new ColorFormulae(ctx);
        cf2.addAssertion(expr);
        System.out.println(cf2);
        System.out.println(cf2.check());

        cf.intersect(cf2);
        System.out.println(cf);
        System.out.println(cf.check());

    }*/
}
