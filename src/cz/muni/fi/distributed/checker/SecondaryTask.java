package cz.muni.fi.distributed.checker;

import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.model.ColorSet;

public class SecondaryTask implements Task {

    private final VerificationTask task;
    private final int taskId;
    private final int parentId;
    private boolean running = false;
    private int source;
    private Node destNode;
    private ColorSet initialColors;

    public SecondaryTask(VerificationTask task, int source, int parentId, Node destNode, ColorSet colorSet) {
        this.task = task;
        this.source = source;
        this.destNode = destNode;
        this.initialColors = colorSet;
        this.taskId = (int) System.nanoTime();
        this.parentId = parentId;
    }

    @Override
    public int getSource() {
        return source;
    }

    @Override
    public int getId() {
        return taskId;
    }

    @Override
    public int getParentId() {
        return parentId;
    }

    @Override
    public void executeAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                running = true;
                new FormulaVerificator(task.getGraph(), task.getDispatcher(), taskId).processFormula(task.getFormula(), destNode, initialColors);
                running = false;
                task.getDispatcher().onLocalTaskFinished(SecondaryTask.this);
            }
        }).start();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
