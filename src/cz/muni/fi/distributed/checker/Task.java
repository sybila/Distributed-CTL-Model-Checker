package cz.muni.fi.distributed.checker;

/**
 * Created by daemontus on 28/11/14.
 */
public interface Task {

    public int getId();
    public int getParentId();
    public int getSource();
    public void executeAsync();
    public boolean isRunning();
}
