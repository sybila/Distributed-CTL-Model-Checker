package cz.muni.fi.frontend;

import cz.muni.fi.ctl.FormulaNormalizer;
import cz.muni.fi.ctl.FormulaParser;
import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelChecker;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.mpi.tasks.BlockingTaskMessenger;
import cz.muni.fi.modelchecker.mpi.tasks.OnTaskListener;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import cz.muni.fi.modelchecker.mpi.termination.MPITokenMessenger;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import cz.muni.fi.thomas.BitMapColorSet;
import cz.muni.fi.thomas.LevelNode;
import cz.muni.fi.thomas.NativeModel;
import cz.muni.fi.thomas.NetworkModel;
import mpi.MPI;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by daemontus on 09/02/15.
 */
public class ThomasMain {

    static {
        NativeUtils.loadLibrary("Thomas");
    }

    public static void main(String[] args) throws IOException {


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
        NativeModel model = new NativeModel(args[args.length - 2]);
        @NotNull StateSpacePartitioner<LevelNode> partitioner = new StateSpacePartitioner<LevelNode>() {
            @Override
            public int getNodeOwner(@NotNull LevelNode node) throws IllegalArgumentException {
                return 0;
            }

            @Override
            public int getMyId() {
                return 0;
            }
        };
        @NotNull NetworkModel factory = new NetworkModel(partitioner);
        model.loadModel(factory);
        factory.printOut();

        //prepare MPI communication environment
        @NotNull Terminator.TerminatorFactory terminatorFactory = new Terminator.TerminatorFactory(new MPITokenMessenger(MPI.COMM_WORLD));
        @NotNull TaskMessenger<LevelNode, BitMapColorSet> taskMessenger = new BlockingTaskMessenger<LevelNode, BitMapColorSet>() {
            @Override
            protected boolean blockingReceiveTask(@NotNull OnTaskListener<LevelNode, BitMapColorSet> taskListener) {
                return false;
            }

            @Override
            protected void finishSelf() {

            }

            @Override
            public void sendTask(int destinationProcess, @NotNull LevelNode internal, @NotNull LevelNode external, @NotNull BitMapColorSet colors) {

            }
        };

        //prepare model checker and run verification
        @NotNull ModelChecker<LevelNode, BitMapColorSet> modelChecker = new ModelChecker<>(factory, partitioner, taskMessenger, terminatorFactory);
        modelChecker.verify(formula);

       /* System.out.println(" ---------  PARAMETER KEY --------- ");
        int[] stepSizes = new int[factory.variableOrdering.size()];
        int meanwhile = 1;
        List<String> varRev = Lists.reverse(factory.variableOrdering);
        for (int s = 0; s < varRev.size(); s++) {
            String var = varRev.get( s );
            stepSizes[s] = meanwhile;
            meanwhile *= model.specieContextTargetMapping.get(var).values().iterator().next().size();
        }
        for (int i = 0; i < factory.paramSpaceWidth; i++) {
            System.out.println("No: "+i);
            for (int j = 0; j < varRev.size(); j++) {
                String var = varRev.get(j);
                System.out.println("Specie: "+var);
                for (Map.Entry<String, List<Byte>> entry : model.specieContextTargetMapping.get(var).entrySet()) {
                    int value_num = (i / stepSizes[j]) % entry.getValue().size();
                    System.out.print(entry.getKey() + ": " + entry.getValue().get(value_num)+" ; ");
                }
                System.out.println();
            }
        }*/

        //print results
        System.out.println(" ---------  RESULTS --------- ");
        if (args.length >= 3 && args[args.length - 3].equals("--all")) {
            for (@NotNull LevelNode node : factory.getNodes()) {
                System.out.println(node.toString());
            }
        } else if (args.length >= 3 && !args[args.length - 3].equals("--none")) {
            for (@NotNull LevelNode node : factory.getNodes()) {
                @NotNull BitMapColorSet colorSet = factory.validColorsFor(node, formula);
                if (!colorSet.isEmpty()) {
                    System.out.println(Arrays.toString(node.levels)+" "+colorSet);
                }
            }
        }

        MPI.Finalize();
        System.err.println(MPI.COMM_WORLD.Rank()+" Duration: "+(System.currentTimeMillis() - start));
        System.exit(0);
    }

}
