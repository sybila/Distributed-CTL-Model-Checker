package cz.muni.fi.distributed.graph;


import com.google.common.collect.Range;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.Model;
import cz.muni.fi.model.StateSpacePartitioner;
import cz.muni.fi.model.TreeColorSet;

import java.util.List;

public class Graph {
    
    public final StateSpacePartitioner partitioner;
    public final NodeFactory factory;
    public final Model model;
    public final int id;

    public Graph(Model model, int size, int rank) {
        this.model = model;
        partitioner = new StateSpacePartitioner(model, size, rank);
        factory = new NodeFactory(this);
        this.id = rank;
    }

    public boolean isMyNode(Node node) {
        return partitioner.getGraphId(node) == id;
    }

    public List<Range<Double>> getLimit() {
        return partitioner.getMyLimit(this);
    }

    public ColorSet getFullParamRange() {
        List<Range<Double>> list = model.getParameterRange();
        ColorSet colorSet = TreeColorSet.createEmpty(model.variableCount());
        for (int i = 0; i < list.size(); i++) {
            Range<Double> item = list.get(i);
            colorSet.get(i).add(item);
        }
        return colorSet;
    }

}
