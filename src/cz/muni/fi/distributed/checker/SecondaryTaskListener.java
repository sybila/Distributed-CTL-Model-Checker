package cz.muni.fi.distributed.checker;

import com.google.common.collect.Range;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.TreeColorSet;
import mpi.Intracomm;
import mpi.MPI;

import java.util.*;

import static cz.muni.fi.distributed.checker.Tag.DISPATCHER_COMMAND;
import static cz.muni.fi.distributed.checker.Tag.DISPATCHER_DATA;

/**
 * Listens for secondary task requests, executes them and keeps track of finished requests
 */
public class SecondaryTaskListener implements Runnable, TaskDispatcher {

    private static final int FINISH = -1;
    private static final int CREATE = -2;
    private final VerificationTask task;
    private final Intracomm COMM;
    private final int dimensions;

    private Thread thread;
    private int runningLocalTasks;
    private Map<Integer, Integer> pendingRequests = new HashMap<>();
    private Map<Integer, Task> waitingForFinish = new HashMap<>();

    public SecondaryTaskListener(Intracomm comm, VerificationTask task) {
        this.task = task;
        this.COMM = comm;
        this.dimensions = task.getGraph().model.variableCount();
    }

    /**
     * Secondary task message structure:
     * coordinates of source node|coordinates of destination node|lengths of color sets for parameters|sender|parentId
     */
    @Override
    public void run() {
        //command buffer
        int[] buffer = new int[3*dimensions + 3];
        while (buffer[0] != FINISH) {
            Log.d(COMM.Rank()+" Waiting for task...");
            COMM.Recv(buffer, 0, buffer.length, MPI.INT, MPI.ANY_SOURCE, DISPATCHER_COMMAND.getTag());
            if (buffer[0] == CREATE) {
                int sender = buffer[buffer.length - 2];
                int parentId = buffer[buffer.length - 1];
                Log.d(COMM.Rank()+" Received ("+COMM.Rank()+"): "+sender);
                //Node source = task.getGraph().factory.getNode(Arrays.copyOfRange(buffer, 1, dimensions + 1));
                Node dest = task.getGraph().factory.getNode(Arrays.copyOfRange(buffer, dimensions + 1, 2*dimensions + 1));
                int[] lengths = Arrays.copyOfRange(buffer, 2*dimensions + 1, 3*dimensions + 1);
                double[] colors = new double[sum(lengths)];
              //  Log.d("Waiting for data...");
                COMM.Recv(colors, 0, colors.length, MPI.DOUBLE, sender, DISPATCHER_COMMAND.getTag());
              //  Log.d("Got data...");
                Log.d(COMM.Rank()+" Data received: "+Arrays.toString(colors));
                ColorSet colorSet = TreeColorSet.createFromBuffer(lengths, colors);
                synchronized (this) {
                    Log.d(COMM.Rank()+" Processing task: "+parentId);
                    new SecondaryTask(task, sender, parentId, dest, colorSet).executeAsync();
                    runningLocalTasks++;
                }
            }
        }
        Log.d("Secondary task listener finished (" + COMM.Rank() + ")");
    }

    public void finishSelf() {
        int[] buffer = new int[3*dimensions + 3];
        buffer[0] = FINISH;
        COMM.Send(buffer, 0, buffer.length, MPI.INT, COMM.Rank(), DISPATCHER_COMMAND.getTag());
    }

    private static int sum(int[] array) {
        int sum = 0;
        for (int item : array) {
            sum += item;
        }
        return sum;
    }

    /**
     * Start new thread with terminator running.
     */
    public SecondaryTaskListener execute() {
        if (thread != null) {
            throw new IllegalStateException("Can't execute same terminator twice");
        }
        thread = new Thread(this);
        thread.start();
        return this;
    }

    /**
     * Wait for terminator to finish
     */
    public void join() throws InterruptedException {
        thread.join();
    }

    @Override
    public void dispatchNewTask(int parentTask, int destination, Node source, Node dest, ColorSet activeColors) {
        int[] buffer = new int[3*dimensions + 3];
        buffer[0] = CREATE;
        buffer[buffer.length - 2] = COMM.Rank();
        buffer[buffer.length - 1] = parentTask;
        System.arraycopy(source.coordinates, 0, buffer, 1, dimensions);
        System.arraycopy(dest.coordinates, 0, buffer, dimensions + 1, dimensions);
        List<Double> data = new ArrayList<>(2*dimensions);
        for (int i=0; i<dimensions; i++) {
            Range<Double>[] ranges = activeColors.asArrayForParam(i);
            buffer[2*dimensions + i + 1] = 2 * ranges.length;
            for (Range<Double> range : ranges) {
                data.add(range.lowerEndpoint());
                data.add(range.upperEndpoint());
            }
        }
        synchronized (this) {
            Log.d(COMM.Rank()+"->"+destination+" Spawning task: "+parentTask+" "+Arrays.toString(data.toArray()));
            COMM.Send(buffer, 0, buffer.length, MPI.INT, destination, DISPATCHER_COMMAND.getTag());
            COMM.Send(toBuffer(data), 0, data.size(), MPI.DOUBLE, destination, DISPATCHER_COMMAND.getTag());
            Integer k = pendingRequests.get(parentTask);
            if (k == null) k = 0;
            Log.d("Adding tasks for parent: "+parentTask+" "+k);
            pendingRequests.put(parentTask, k+1);
        }
    }

    private static double[] toBuffer(List<Double> integers)
    {
        double[] ret = new double[integers.size()];
        Iterator<Double> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next();
        }
        return ret;
    }

    @Override
    public synchronized boolean hasIncompleteRequests() {
        return runningLocalTasks > 0 || !pendingRequests.isEmpty();
    }

    @Override
    public synchronized void onLocalTaskFinished(Task task) {
        Log.d(COMM.Rank()+" Local task finished: "+task.getId());
        if (pendingRequests.containsKey(task.getId())) {
            waitingForFinish.put(task.getId(), task);
        } else {
            Log.d(COMM.Rank()+" Task terminated: "+task.getId());
            this.task.getTerminator().sendTaskCompletedNotification(task);
        }
        runningLocalTasks--;
    }

    @Override
    public synchronized void onRemoteTaskFinished(int parentTask) {
        Integer k = pendingRequests.get(parentTask);
        Log.d(COMM.Rank()+" Remote task finished: "+parentTask);
        if (k == null) {
            throw new IllegalStateException("Cannot finish task - parent task has no running tasks");
        } else if (k == 1) {
            pendingRequests.remove(parentTask);
            if (waitingForFinish.containsKey(parentTask)) {
                Log.d(COMM.Rank()+" Task terminated: "+waitingForFinish.get(parentTask).getId());
                this.task.getTerminator().sendTaskCompletedNotification(waitingForFinish.get(parentTask));
            }
        } else {
            pendingRequests.put(parentTask, k-1);
        }
        Log.d(COMM.Rank()+" Removing tasks for parent: "+parentTask+" "+k);
    }
}
