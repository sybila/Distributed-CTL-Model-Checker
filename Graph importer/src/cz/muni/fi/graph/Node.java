package cz.muni.fi.graph;

import cz.muni.fi.ctl.formula.Formula;

import java.util.*;

public class Node {

    public int visits = 0;
    private List<Path> from = new ArrayList<>();    //parents
    private List<Path> to = new ArrayList<>();  //children
    private float[] values;
    public Map<Formula, List<Integer>> correct = new HashMap<>();

    public Node(float[] values) {
        this.values = values;
    }

    public void addAfter(Path path) {
        to.add(path);
    }

    public void addBefore(Path path) {
        from.add(path);
    }

    public List<Path> getBefore() {
        return from;
    }

    public List<Path> getAfter() {
        return to;
    }

    @Override
    public String toString() {
        return "Node "+ Arrays.toString(values);
    }
}
