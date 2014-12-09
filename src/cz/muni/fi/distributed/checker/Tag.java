package cz.muni.fi.distributed.checker;

/**
 * Created by daemontus on 24/11/14.
 */
public enum Tag {
    TERMINATOR(1), DISPATCHER_COMMAND(2), DISPATCHER_DATA(3);
    private int tag;

    private Tag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }
}
