package cz.muni.fi.frontend;

import cz.muni.fi.ctl.FormulaNormalizer;
import cz.muni.fi.ctl.FormulaParser;
import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelChecker;
import cz.muni.fi.modelchecker.mpi.TaskManager;
import cz.muni.fi.ode.*;
import mpi.MPI;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class Main {

    static {
        try {
            System.loadLibrary("generator"); // used for tests. This library in classpath only
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Link error. Using lib from zip file.");
            try {
                NativeUtils.loadLibraryFromJar("/build/binaries/libgenerator.jnilib"); // during runtime. .DLL within .JAR
            } catch (IOException e1) {
                try {
                    NativeUtils.loadLibraryFromJar("/build/binaries/libgenerator.so");
                } catch (IOException e2) {
                    String property = System.getProperty("java.library.path");
                    StringTokenizer parser = new StringTokenizer(property, ";");
                    while (parser.hasMoreTokens()) {
                        System.err.println(parser.nextToken());
                    }
                    System.exit(1);
                }
            }
        }
    }

    public static void main(String[] args) {
        MPI.Init(args);
        FormulaParser parser = new FormulaParser();
        FormulaNormalizer normalizer = new FormulaNormalizer();
        try {
            Formula formula = parser.parse(new File(args[args.length - 1]));
            formula = normalizer.normalize(formula);
            System.out.println("Normalized form: "+formula);
            OdeModel model = new OdeModel(args[args.length - 2]);
            model.load();
            RectangularPartitioner partitioner = new RectangularPartitioner(model, MPI.COMM_WORLD.Size(), MPI.COMM_WORLD.Rank());
            NodeFactory factory = new NodeFactory(model, partitioner);
            StateSpaceGenerator generator = new StateSpaceGenerator(model, true, factory);
            factory.setGenerator(generator);
/*
            for (List<SumMember> members : model.equations) {
                for (SumMember member : members) {
                    System.out.println(member.toString());
                }
            }
*/
            int s = 0;
            factory.cacheAllNodes(partitioner.getMyLimit());
            System.out.println("Got nodes");
            int k = 0;
            int p = 0;
            int size = factory.nodeCache.size();
            for (Map.Entry<Integer, CoordinateNode> node : factory.nodeCache.entrySet()) {
                Map<CoordinateNode, TreeColorSet> data = generator.getSuccessors(node.getValue(), model.getFullColorSet());
                s += data.size();
                k++;
                if (k > size/50) {
                    k = 0;
                    p += 2;
                    System.out.println(p + "%...");
                }
            }
            System.out.println("sum: "+s);

            /*for (List<Double> thresholds : model.thresholds) {
                for (int i=0; i<thresholds.size()-1; i++) {
                    CoordinateNode node = factory.getNode(new int[]{i});
                    Map<CoordinateNode, TreeColorSet> data = generator.getSuccessors(node, model.getFullColorSet());
                    System.out.println("Succ for: "+node);
                    for (Map.Entry<CoordinateNode, TreeColorSet> entry : data.entrySet())
                    {
                        System.out.println(entry.getKey()+" "+entry.getValue());
                    }
                }
            }*/


          /*  TaskManager.TaskManagerFactory<CoordinateNode, TreeColorSet> taskFactory = new MpiTaskManager.MpiTaskManagerFactory(model.variableCount(), factory, model);
            ModelChecker<CoordinateNode, TreeColorSet> modelChecker = new ModelChecker<>(factory, partitioner, taskFactory, MPI.COMM_WORLD);
            modelChecker.verify(formula);
            if (args.length >= 3 && args[args.length - 3].equals("-all")) {
                for (CoordinateNode node : factory.getNodes()) {
                    System.out.println(node.toString());
                }
            } else {
                for (CoordinateNode node : factory.getNodes()) {
                    TreeColorSet colorSet = factory.validColorsFor(node, formula);
                    if (!colorSet.isEmpty()) {
                        System.out.println(Arrays.toString(node.coordinates)+" "+colorSet);
                    }
                }
            }*/
        } catch (IOException e) {
            e.printStackTrace();
        }
        MPI.Finalize();
    }
}
