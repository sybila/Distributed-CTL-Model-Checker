package cz.muni.fi.frontend;

import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    
    static {
        loadLibrary("ODE");
        loadLibrary("Thomas");
    }

    public static void main(@NotNull String[] args) throws InterruptedException, IOException {

        Range<Double> first = Range.open(0.0,10.0);
        Range<Double> second = Range.open(0.0,10.0);
        Set<Range<Double>> fs = new HashSet<>();
        fs.add(first);
        Set<Range<Double>> st = new HashSet<>();
        st.add(second);
        List<Set<Range<Double>>> fst = new ArrayList<>();
        fst.add(fs);
        List<Set<Range<Double>>> scn = new ArrayList<>();
        scn.add(st);
        System.out.println("EQ: "+first.equals(second));
        System.out.println("EQ: "+fs.equals(st));
        System.out.println("Hash: "+fs.hashCode()+" "+st.hashCode());
        System.out.println("Hash: "+scn.hashCode()+" "+fst.hashCode());
/*
        //TODO: Now this is just ODE main, but in the future, this should be built into interactive console application.

        //prepare benchmark
        long start = System.currentTimeMillis();

        //start MPI
        MPI.Init(args);
        if (MPI.COMM_WORLD.Rank() == 0) {
            System.out.println("MPI started on "+MPI.COMM_WORLD.Size()+" machines.");
        }

        //read and normalize formula
        @NotNull FormulaParser parser = new FormulaParser();
        @NotNull FormulaNormalizer normalizer = new FormulaNormalizer();
        @NotNull Formula formula = parser.parse(new File(args[args.length - 1]));
        formula = normalizer.normalize(formula);
        if (MPI.COMM_WORLD.Rank() == 0) {
            System.out.println("Formula prepared for verification: "+formula);
        }

        //read and prepare model
        @NotNull OdeModel model = new OdeModel(args[args.length - 2]);
        model.load();

        //@NotNull CoordinatePartitioner partitioner = new RectangularPartitioner(model, MPI.COMM_WORLD.Size(), MPI.COMM_WORLD.Rank());
        @NotNull CoordinatePartitioner partitioner = new HashPartitioner(model, MPI.COMM_WORLD.Size(), MPI.COMM_WORLD.Rank());
        @NotNull NodeFactory factory = new NodeFactory(model, partitioner);
        @NotNull StateSpaceGenerator generator = new StateSpaceGenerator(model, true, factory);
        factory.setGenerator(generator);

        //prepare MPI communication environment
        @NotNull Terminator.TerminatorFactory terminatorFactory = new Terminator.TerminatorFactory(new MPITokenMessenger(MPI.COMM_WORLD));
        @NotNull TaskMessenger<CoordinateNode, TreeColorSet> taskMessenger = new MpiTaskMessenger(MPI.COMM_WORLD, model.variableCount(), factory, model);

        //prepare model checker and run verification
        @NotNull ModelChecker<CoordinateNode, TreeColorSet> modelChecker = new ModelChecker<>(factory, partitioner, taskMessenger, terminatorFactory);
        modelChecker.verify(formula);

        //print results
        if (args.length >= 3 && args[args.length - 3].equals("--all")) {
            for (@NotNull CoordinateNode node : factory.getNodes()) {
                System.out.println(node.toString());
            }
        } else if (args.length >= 3 && !args[args.length - 3].equals("--none")) {
            for (@NotNull CoordinateNode node : factory.getNodes()) {
                @NotNull TreeColorSet colorSet = factory.validColorsFor(node, formula);
                if (!colorSet.isEmpty()) {
                    System.out.println(Arrays.toString(node.coordinates)+" "+colorSet);
                }
            }
        }

        MPI.Finalize();
        System.err.println(MPI.COMM_WORLD.Rank()+" Duration: "+(System.currentTimeMillis() - start));
        System.exit(0);*/

    }


    private static void loadLibrary(String name) {
        try {
            System.loadLibrary(name);
            System.out.println(name+" module loaded from include path.");
        } catch (UnsatisfiedLinkError e) {
            try {
                switch (OsCheck.getOperatingSystemType()) {
                    case MacOS:
                        NativeUtils.loadLibraryFromJar("/lib"+name+".jnilib");
                        break;
                    case Linux:
                        NativeUtils.loadLibraryFromJar("/lib"+name+".so");
                        break;
                    default:
                        System.out.println("Unsupported operating system for module: "+name);
                        break;
                }
                System.out.println(name+" module loaded from jar file.");
            } catch (Exception e1) {
                System.out.println("Unable to load module: "+name+", problem: "+e1.toString());
            }
        }
    }

}
