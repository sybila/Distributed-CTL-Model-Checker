package cz.muni.fi.graph;

import java.util.Set;

/**
 * Created by daemontus on 14/10/14.
 */
public class SubGraph extends Graph {

    private int subGraphId;

    public SubGraph(Set<Integer> parameterSet, int subGraphId) {
        super(parameterSet);
        this.subGraphId = subGraphId;
    }


    public int getSubGraphId() {
        return subGraphId;
    }
}
