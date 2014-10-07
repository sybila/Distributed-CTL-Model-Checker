package cz.muni.fi.graph;

/**
 * Created by daemontus on 23/09/14.
 */
public class Path {

    private Node from;
    private Node to;
    private int color;

    public Path(Node from, Node to, int color) {
        this.from = from;
        this.to = to;
        this.color = color;
    }

    public Node getFrom() {
        return from;
    }

    public void setFrom(Node from) {
        this.from = from;
    }

    public Node getTo() {
        return to;
    }

    public void setTo(Node to) {
        this.to = to;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
