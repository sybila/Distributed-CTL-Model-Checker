package cz.muni.fi.ode;

import com.microsoft.z3.Context;
import cz.muni.fi.modelchecker.mpi.tasks.BlockingTaskMessenger;
import cz.muni.fi.modelchecker.mpi.tasks.OnTaskListener;
import mpi.Comm;
import mpi.MPI;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Listens for secondary task requests, executes them and keeps track of finished requests
 */
public class MpiTaskMessenger extends BlockingTaskMessenger<CoordinateNode, ColorFormulae> {

    public static long messageCount = 0;
    public static long messageSize = 0;
    public static long parserTime = 0;

    private static final int TAG = 1;

    private static final int FINISH = -1;
    private static final int CREATE = -2;

    private final Comm COMM;
    private final int dimensions;
    private final NodeFactory factory;
    private final OdeModel model;

    private final Context solverContext;

    private final int[] recvBuffer;
    private double[] recvParams;

    public MpiTaskMessenger(
            Comm comm,
            int dimensions,
            NodeFactory factory,
            OdeModel model
    ) {
        this.COMM = comm;
        this.dimensions = dimensions;
        this.factory = factory;
        this.model = model;
        this.solverContext = model.getDefaultContext();
        this.recvBuffer = new int[2*dimensions + 4];
        this.recvParams = new double[2];
    }

    /**
     * Secondary task message structure:
     * coordinates of source node|coordinates of destination node|lengths of color string|sender|parentId
     */


    @Override
    public void sendTask(int destinationNode, @NotNull CoordinateNode internal, @NotNull CoordinateNode external, @NotNull ColorFormulae colors) {
        @NotNull int[] buffer = new int[2*dimensions + 4];
        buffer[0] = CREATE;
        buffer[buffer.length - 2] = COMM.Rank();
        buffer[buffer.length - 1] = 0;
        System.arraycopy(internal.coordinates, 0, buffer, 1, dimensions);
        System.arraycopy(external.coordinates, 0, buffer, dimensions + 1, dimensions);
        String parameterFormula = colors.toString();
        buffer[buffer.length - 3] = parameterFormula.length();
        //@NotNull List<Double> data = new ArrayList<>(2*dimensions);
        /*//TODO: temporary - this class is not necessary right now
        for (int i=0; i<model.parameterCount(); i++) {
            @NotNull Range<Double>[] ranges = colors.asArrayForParam(i);
            buffer[2*dimensions + i + 1] = 2 * ranges.length;
            for (@NotNull Range<Double> range : ranges) {
                data.add(range.lowerEndpoint());
                data.add(range.upperEndpoint());
            }
        }
        */
        char[] chars = parameterFormula.toCharArray();
        synchronized (this) {   //ensure message ordering
            messageCount += 2;
            messageSize += 4 * buffer.length;
            messageSize += chars.length;
            COMM.Bsend(buffer, 0, buffer.length, MPI.INT, destinationNode, TAG);
            COMM.Bsend(chars, 0, chars.length, MPI.CHAR, destinationNode, TAG);
        }
    }

    @Override
    protected boolean blockingReceiveTask(@NotNull OnTaskListener<CoordinateNode, ColorFormulae> taskListener) {
        //no need to synchronize - this method is only called from one thread
        COMM.Recv(recvBuffer, 0, recvBuffer.length, MPI.INT, MPI.ANY_SOURCE, TAG);
        if (recvBuffer[0] == CREATE) {
            int sender = recvBuffer[recvBuffer.length - 2];
            //int parentId = buffer[buffer.length - 1];
            CoordinateNode source = factory.getNode(Arrays.copyOfRange(recvBuffer, 1, dimensions + 1));
            CoordinateNode dest = factory.getNode(Arrays.copyOfRange(recvBuffer, dimensions + 1, 2 * dimensions + 1));
            int length = recvBuffer[recvBuffer.length - 3];
            char[] chars = new char[length];
            COMM.Recv(chars, 0, length, MPI.CHAR, sender, TAG);
            String formula = String.valueOf(chars);
            String full = model.getSmtParamDefinition() + "( assert "+formula+" )";
	    // System.out.println(full);
            long start = System.currentTimeMillis();
            @NotNull ColorFormulae colorSet = new ColorFormulae(
                    solverContext,
                    model.getDefaultSolver(),
                    model.getDefaultGoal(),
                    model.getDefaultTactic(),
                    solverContext.parseSMTLIB2String(
                            full,
                            null, null, null, null)
                    );
            parserTime += System.currentTimeMillis() - start;
            taskListener.onTask(sender, source, dest, colorSet);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void finishSelf() {
        @NotNull int[] buffer = new int[2*dimensions + 4];
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
