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

    private final int[] recvBuffer;
    private double[] recvParams;

    public MpiTaskMessenger(
            Comm comm,
            int dimensions,
            NodeFactory factory,
            OdeModel model) {
        this.COMM = comm;
        this.dimensions = dimensions;
        this.factory = factory;
        this.model = model;
        this.recvBuffer = new int[2*dimensions + model.parameterCount() + 3];
        this.recvParams = new double[2];
    }

    /**
     * Secondary task message structure:
     * coordinates of source node|coordinates of destination node|lengths of color sets for parameters|sender|parentId
     */


    @Override
    public void sendTask(int destinationNode, @NotNull CoordinateNode internal, @NotNull CoordinateNode external, @NotNull TreeColorSet colors) {
        @NotNull int[] buffer = new int[2*dimensions + model.parameterCount() + 3];
        buffer[0] = CREATE;
        buffer[buffer.length - 2] = COMM.Rank();
        buffer[buffer.length - 1] = 0;
        System.arraycopy(internal.coordinates, 0, buffer, 1, dimensions);
        System.arraycopy(external.coordinates, 0, buffer, dimensions + 1, dimensions);
        @NotNull List<Double> data = new ArrayList<>(2*dimensions);
        for (int i=0; i<model.parameterCount(); i++) {
            @NotNull Range<Double>[] ranges = colors.asArrayForParam(i);
            buffer[2*dimensions + i + 1] = 2 * ranges.length;
            for (@NotNull Range<Double> range : ranges) {
                data.add(range.lowerEndpoint());
                data.add(range.upperEndpoint());
            }
        }
        synchronized (this) {   //ensure message ordering
            COMM.Bsend(buffer, 0, buffer.length, MPI.INT, destinationNode, TAG);
            COMM.Bsend(toBuffer(data), 0, data.size(), MPI.DOUBLE, destinationNode, TAG);
        }
    }

    @Override
    protected boolean blockingReceiveTask(@NotNull OnTaskListener<CoordinateNode, TreeColorSet> taskListener) {
        //no need to synchronize - this method is only called from one thread
        COMM.Recv(recvBuffer, 0, recvBuffer.length, MPI.INT, MPI.ANY_SOURCE, TAG);
        if (recvBuffer[0] == CREATE) {
            int sender = recvBuffer[recvBuffer.length - 2];
            //int parentId = buffer[buffer.length - 1];
            CoordinateNode source = factory.getNode(Arrays.copyOfRange(recvBuffer, 1, dimensions + 1));
            CoordinateNode dest = factory.getNode(Arrays.copyOfRange(recvBuffer, dimensions + 1, 2 * dimensions + 1));
            @NotNull int[] lengths = Arrays.copyOfRange(recvBuffer, 2*dimensions + 1, 2*dimensions + model.parameterCount() + 1);
            //@NotNull double[] colors = new double[sum(lengths)];
            int paramBufferSize = sum(lengths);

            if (recvParams.length < paramBufferSize) {
                recvParams = new double[paramBufferSize];
            }

            COMM.Recv(recvParams, 0, paramBufferSize, MPI.DOUBLE, sender, TAG);
            @NotNull TreeColorSet colorSet = TreeColorSet.createFromBuffer(lengths, recvParams);
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
        COMM.Bsend(buffer, 0, buffer.length, MPI.INT, (COMM.Rank() + 1) % COMM.Size(), TAG);
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
