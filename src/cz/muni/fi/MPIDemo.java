package cz.muni.fi;

import cz.adamh.utils.NativeUtils;
import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.FormulaImpl;
import cz.muni.fi.ctl.formula.operator.UnaryOperator;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.ctl.parser.CTLParser;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.distributed.graph.Graph;
import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.Model;
import mpi.MPI;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;

public class MPIDemo {

    public static void main(@NotNull String[] args) throws InterruptedException {
        Log.d(System.getProperty("java.version"));
        MPI.Init(args);
        Log.d("Formula: "+args[args.length-1]);
        Formula formula = CTLParser.parse(args[args.length-1]);
        //FloatProposition proposition = new FloatProposition(10f, "pRB", FloatProposition.Operator.LT_EQ);
        //Formula formula = new FormulaImpl(UnaryOperator.EXISTS_NEXT, proposition);
        //Model model = new Model("/Users/daemontus/Synced/Java Development/Parametrized CTL Model Checker/native/examples/positive_feedback_ramp.bio");
        @NotNull Model model = new Model(args[args.length-2]);
        model.load();
        @NotNull DistributedModelChecker checker = new DistributedModelChecker(model, MPI.COMM_WORLD.Size(), MPI.COMM_WORLD.Rank());
        checker.check(formula);
        for (@NotNull Node node : checker.graph.factory.nodeCache.values()) {
            Log.i(Arrays.toString(node.coordinates)+" formulae: "+node.formulae.toString());
        }
        //Log.d(checker.graph.factory.getNode(new int[] {0}).formulae.toString());
        //Log.d(checker.graph.factory.getNode(new int[] {1}).formulae.toString());
        //Log.d(checker.graph.factory.getNode(new int[] {2}).formulae.toString());
        MPI.Finalize();
    }

    static {
        try {
            System.loadLibrary("generator"); // used for tests. This library in classpath only
        } catch (UnsatisfiedLinkError e) {
            Log.d("Cannot link");
            try {
                NativeUtils.loadLibraryFromJar("/libgenerator.jnilib"); // during runtime. .DLL within .JAR
            } catch (IOException e1) {
                Log.d("No lib in zip");
                String property = System.getProperty("java.library.path");
                @NotNull StringTokenizer parser = new StringTokenizer(property, ";");
                while (parser.hasMoreTokens()) {
                    System.err.println(parser.nextToken());
                }
                System.exit(1);
            }
        }
    }

}

