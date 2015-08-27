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
        NativeUtils.loadLibrary("ODE");
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
        @NotNull TaskMessenger<CoordinateNode, RectParamSpace> taskMessenger = new MpiTaskMessenger(MPI.COMM_WORLD, model.getVariableCount(), factory, model);

        //prepare model checker and run verification
        @NotNull ModelChecker<CoordinateNode, RectParamSpace> modelChecker = new ModelChecker<>(factory, partitioner, taskMessenger, terminatorFactory);
        modelChecker.verify(formula);

        /*for (CoordinateNode node : factory.getNodes()) {
            TreeColorSet result = model.getFullColorSet();
            Map<CoordinateNode, TreeColorSet> successors = generator.getSuccessors(node, model.getFullColorSet());
            for (Map.Entry<CoordinateNode, TreeColorSet> suc : successors.entrySet()) {
                if (!suc.getKey().equals(node)) {
                    result.subtract(suc.getValue());
                }
            }
            if (!result.isEmpty()) {
                System.out.println("Stab: "+model.coordinateString(node.coordinates)+" "+result+" "+successors.containsKey(node));
            }
        }*/

        //print results
        if (args.length >= 3 && args[args.length - 3].equals("--all")) {
            for (@NotNull CoordinateNode node : factory.getNodes()) {
                System.out.println(node.fullString());
            }
        } else if (args.length >= 3 && !args[args.length - 3].equals("--none")) {
            for (@NotNull CoordinateNode node : factory.getNodes()) {
                @NotNull RectParamSpace colorSet = factory.validColorsFor(node, formula);
                if (!colorSet.isEmpty()) {
                    System.out.println(model.coordinateString(node.coordinates)+" "+colorSet);
                }
            }
        }

        MPI.Buffer_detach();

        MPI.Finalize();
        System.err.println(MPI.COMM_WORLD.Rank()+" Duration: "+(System.currentTimeMillis() - start));
        System.exit(0);
    }

}
