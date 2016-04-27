package cz.muni.fi.frontend;

import cz.muni.fi.ctl.FormulaNormalizer;
import cz.muni.fi.ctl.FormulaParser;
import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelChecker;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import cz.muni.fi.modelchecker.mpi.termination.MPITokenMessenger;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import cz.muni.fi.ode.*;
import mpi.MPI;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ODEMain {

    static {
        //System.out.println(System.getProperty("java.library.path"));
        NativeUtils.loadLibrary("ODE");
        NativeUtils.loadLibrary("z3");  //windows does not like the java suffix.
        NativeUtils.loadLibrary("z3java");
    }

    public static void main(@NotNull String[] args) throws InterruptedException, IOException {

        //prepare benchmark
        long start = System.currentTimeMillis();

        //start MPI
        MPI.Init(args);
        if (MPI.COMM_WORLD.Rank() == 0) {
            System.out.println("MPI started on "+MPI.COMM_WORLD.Size()+" machines.");
        }

        MPI.Buffer_attach(ByteBuffer.allocateDirect(1000 * 1000));

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
        @NotNull HashPartitioner partitioner = new HashPartitioner(model, MPI.COMM_WORLD.Size(), MPI.COMM_WORLD.Rank());
        @NotNull NodeFactory factory = new NodeFactory(model, partitioner);
        @NotNull StateSpaceGenerator generator = new StateSpaceGenerator(model, factory, partitioner.getMyLimit());
        factory.setGenerator(generator);

        //prepare MPI communication environment
        @NotNull Terminator.TerminatorFactory terminatorFactory = new Terminator.TerminatorFactory(new MPITokenMessenger(MPI.COMM_WORLD));
        @NotNull TaskMessenger<CoordinateNode, ColorFormulae> taskMessenger = new MpiTaskMessenger(MPI.COMM_WORLD, model.getVariableCount(), factory, model);

        //prepare model checker and run verification
        @NotNull ModelChecker<CoordinateNode, ColorFormulae> modelChecker = new ModelChecker<>(factory, partitioner, taskMessenger, terminatorFactory);
        modelChecker.verify(formula);

        /*for (CoordinateNode node : factory.getNodes()) {
            ColorFormulae result = model.getFullColorSet();
            Map<CoordinateNode, ColorFormulae> successors = generator.getSuccessors(node, model.getFullColorSet());
            for (Map.Entry<CoordinateNode, ColorFormulae> suc : successors.entrySet()) {
                if (!suc.getKey().equals(node)) {
                    result.subtract(suc.getValue());
                }
            }
            if (!result.isEmpty()) {
                System.out.println("Stab: "+model.coordinateString(node.coordinates)+" "+result+" "+successors.containsKey(node));
            }
        }*/

        ColorFormulae parameterBounds = model.getParameterBounds();
        //print results
        if (args.length >= 3 && args[args.length - 3].equals("--all")) {
            for (@NotNull CoordinateNode node : factory.getNodes()) {
                System.out.println(node.fullString());
            }
        } else if (args.length >= 3 && !args[args.length - 3].equals("--none")) {
            for (@NotNull CoordinateNode node : factory.getNodes()) {
                @NotNull ColorFormulae colorSet = factory.validColorsFor(node, formula);
                colorSet.intersect(parameterBounds);
                if (!colorSet.isEmpty()) {
                    System.out.println(model.coordinateString(node.coordinates)+" "+colorSet);
                }
            }
        }

        MPI.Buffer_detach();

        MPI.Finalize();
        System.err.println(MPI.COMM_WORLD.Rank()+" Total duration: "+(System.currentTimeMillis() - start));
        System.err.println(MPI.COMM_WORLD.Rank()+" Solver used x-times: "+ColorFormulae.solverUsedCount);
        System.err.println(MPI.COMM_WORLD.Rank()+" Time in solver: "+ColorFormulae.timeSpentInSolver);
        System.err.println(MPI.COMM_WORLD.Rank()+" Messages sent: "+MpiTaskMessenger.messageCount);
        System.err.println(MPI.COMM_WORLD.Rank()+" Aprox. messages size (bytes): "+MpiTaskMessenger.messageSize);
        System.err.println(MPI.COMM_WORLD.Rank()+" Parsing time: "+MpiTaskMessenger.parserTime);
        System.err.println(MPI.COMM_WORLD.Rank()+" Merged: "+ModelChecker.merged);
       // System.err.println(MPI.COMM_WORLD.Rank()+" Solver cache hit: "+ColorFormulae.cacheHit);
        System.exit(0);
    }

}
