package cz.muni.fi.ode;

import com.google.common.collect.Range;
import cz.muni.fi.modelchecker.mpi.tasks.BlockingTaskMessenger;
import cz.muni.fi.modelchecker.mpi.tasks.OnTaskListener;
import mpi.Comm;
import mpi.MPI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Listens for secondary task requests, executes them and keeps track of finished requests
 */
public class MpiTaskMessenger extends BlockingTaskMessenger<CoordinateNode, TreeColorSet> {

    private static final int TAG = 1;

    private static final int FINISH = -1;
    private static final int CREATE = -2;

    private final Comm COMM;
    private final int dimensions;
    private final NodeFactory factory;
    private final OdeModel model;

    public MpiTaskMessenger(
            Comm comm,
            int dimensions,
            NodeFactory factory,
            OdeModel model) {
        this.COMM = comm;
        this.dimensions = dimensions;
        this.factory = factory;
        this.model = model;
    }

    /**
     * Secondary task message structure:
     * coordinates of source node|coordinates of destination node|lengths of color sets for parameters|sender|parentId
     */


    @Override
    public void sendTask(int destinationNode, CoordinateNode internal, CoordinateNode external, TreeColorSet colors) {
        @NotNull int[] buffer = new int[2*dimensions + model.parameterCount() + 3];
        buffer[0] = CREATE;
        buffer[buffer.length - 2] = COMM.Rank();
        buffer[buffer.length - 1] = 0;
        System.arraycopy(internal.coordinates, 0, buffer, 1, dimensions);
        System.arraycopy(external.coordinates, 0, buffer, dimensions + 1, dimensions);
        @NotNull List<Double> data = new ArrayList<>(2*dimensions);
        for (int i=0; i<model.parameterCount(); i++) {
            Range<Double>[] ranges = colors.asArrayForParam(i);
            buffer[2*dimensions + i + 1] = 2 * ranges.length;
            for (@NotNull Range<Double> range : ranges) {
                data.add(range.lowerEndpoint());
                data.add(range.upperEndpoint());
            }
        }
        synchronized (this) {   //ensure message ordering
            COMM.Isend(buffer, 0, buffer.length, MPI.INT, destinationNode, TAG);
            COMM.Isend(toBuffer(data), 0, data.size(), MPI.DOUBLE, destinationNode, TAG);
        }
    }

    @Override
    protected boolean blockingReceiveTask(OnTaskListener<CoordinateNode, TreeColorSet> taskListener) {
        @NotNull int[] buffer = new int[2*dimensions + model.parameterCount() + 3];
        //no need to synchronize - this method is only called from one thread
        COMM.Recv(buffer, 0, buffer.length, MPI.INT, MPI.ANY_SOURCE, TAG);
        if (buffer[0] == CREATE) {
            int sender = buffer[buffer.length - 2];
            //int parentId = buffer[buffer.length - 1];
            CoordinateNode source = factory.getNode(Arrays.copyOfRange(buffer, 1, dimensions + 1));
            CoordinateNode dest = factory.getNode(Arrays.copyOfRange(buffer, dimensions + 1, 2 * dimensions + 1));
            int[] lengths = Arrays.copyOfRange(buffer, 2*dimensions + 1, 2*dimensions + model.parameterCount() + 1);
            double[] colors = new double[sum(lengths)];
            COMM.Recv(colors, 0, colors.length, MPI.DOUBLE, sender, TAG);
            TreeColorSet colorSet = TreeColorSet.createFromBuffer(lengths, colors);
            taskListener.onTask(sender, source, dest, colorSet);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void finishSelf() {
        @NotNull int[] buffer = new int[2*dimensions + model.parameterCount() + 3];
        buffer[0] = FINISH;
        //we have to finish other nodes because we can't send messages to ourselves (BUG)
        COMM.Isend(buffer, 0, buffer.length, MPI.INT, (COMM.Rank() + 1) % COMM.Size(), TAG);
    }

    private static int sum(@NotNull int[] array) {
        int sum = 0;
        for (int item : array) {
            sum += item;
        }
        return sum;
    }

    @NotNull
    private static double[] toBuffer(@NotNull List<Double> integers)
    {
        @NotNull double[] ret = new double[integers.size()];
        @NotNull Iterator<Double> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next();
        }
        return ret;
    }

}
