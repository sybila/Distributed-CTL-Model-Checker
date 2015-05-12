package cz.muni.fi.frontend;

import cz.muni.fi.ctl.formula.proposition.Tautology;
import cz.muni.fi.ode.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

/**
 * Created by daemontus on 12/05/15.
 */
public class TransitionMain {

    static {
        NativeUtils.loadLibrary("ODE");
    }

    public static void main(@NotNull String[] args) throws InterruptedException, IOException {

        //read and prepare model
        @NotNull OdeModel model = new OdeModel(args[args.length - 1]);
        model.load();

        //@NotNull CoordinatePartitioner partitioner = new RectangularPartitioner(model, MPI.COMM_WORLD.Size(), MPI.COMM_WORLD.Rank());
        @NotNull HashPartitioner partitioner = new HashPartitioner(model, 1, 0);
        @NotNull NodeFactory factory = new NodeFactory(model, partitioner);
        @NotNull StateSpaceGenerator generator = new StateSpaceGenerator(model, factory, partitioner.getMyLimit());
        factory.setGenerator(generator);

        //prepare MPI communication environment
        //@NotNull Terminator.TerminatorFactory terminatorFactory = new Terminator.TerminatorFactory(new MPITokenMessenger(MPI.COMM_WORLD));
        //@NotNull TaskMessenger<CoordinateNode, TreeColorSet> taskMessenger = new MpiTaskMessenger(MPI.COMM_WORLD, model.getVariableCount(), factory, model);

        //prepare model checker and run verification
       // @NotNull ModelChecker<CoordinateNode, TreeColorSet> modelChecker = new ModelChecker<>(factory, partitioner, taskMessenger, terminatorFactory);
        //modelChecker.verify(formula);
        // generator.cacheAllNodes();

        for (int i=0; i<model.getVariableCount(); i++) {
            System.out.println("Thresholds for var "+i+": ");
            for (int j=0; j<model.getThresholdCountForVariable(i); j++) {
                System.out.print(model.getThresholdValueForVariableByIndex(i, j) + ",");
            }
            System.out.println();
        }

        for (Map.Entry<CoordinateNode, TreeColorSet> data : factory.initialNodes(Tautology.INSTANCE).entrySet()) {
            System.out.println("From " + data.getKey() + ": ");
            for (Map.Entry<CoordinateNode, TreeColorSet> succ : factory.successorsFor(data.getKey(), null).entrySet()) {
                System.out.println(succ.getKey()+": "+succ.getValue());
            }
        }

        //print results
        /*if (args.length >= 3 && args[args.length - 3].equals("--all")) {
            for (@NotNull CoordinateNode node : factory.getNodes()) {
                System.out.println(node.fullString());
            }
        } else if (args.length >= 3 && !args[args.length - 3].equals("--none")) {
            for (@NotNull CoordinateNode node : factory.getNodes()) {
                @NotNull TreeColorSet colorSet = factory.validColorsFor(node, formula);
                if (!colorSet.isEmpty()) {
                    System.out.println(model.coordinateString(node.coordinates)+" "+colorSet);
                }
            }
        }*/

    }

}
